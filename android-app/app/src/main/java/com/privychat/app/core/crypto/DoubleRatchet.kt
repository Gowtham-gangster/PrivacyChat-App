package com.privychat.app.core.crypto

import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class DoubleRatchet(
    private var rootKey: ByteArray,
    private var dhSendingPrivateKey: X25519PrivateKeyParameters,
    private var dhSendingPublicKey: X25519PublicKeyParameters,
    private var dhReceivingPublicKey: X25519PublicKeyParameters?
) {
    private var sendingChainKey: ByteArray? = null
    private var receivingChainKey: ByteArray? = null
    private var sendingMessageNumber: Long = 0
    private var receivingMessageNumber: Long = 0
    private var previousSendingChainLength: Long = 0

    // Skipped message keys: (RatchetPublicKeyBase64, MessageNumber) -> MessageKey
    private val skippedMessageKeys = mutableMapOf<Pair<String, Long>, ByteArray>()

    private val secureRandom = SecureRandom()

    data class RatchetMessage(
        val ratchetPublicKey: ByteArray,
        val sequenceNumber: Long,
        val previousCounter: Int,
        val ciphertext: ByteArray,
        val iv: ByteArray
    )

    fun encrypt(plaintext: ByteArray): RatchetMessage {
        if (sendingChainKey == null) {
            // Initialize sending chain using DH agreement if receiving key is available
            val dhSecret = computeDH(dhSendingPrivateKey, dhReceivingPublicKey!!)
            val kdfOut = kdfRoot(rootKey, dhSecret)
            rootKey = kdfOut.first
            sendingChainKey = kdfOut.second
        }

        // Advance sending chain
        val messageKey = kdfChainKey(sendingChainKey!!)
        sendingChainKey = kdfNextChainKey(sendingChainKey!!)
        val currentNumber = sendingMessageNumber++

        // Encrypt with AES-GCM
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(messageKey, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val ciphertext = cipher.doFinal(plaintext)

        return RatchetMessage(
            ratchetPublicKey = dhSendingPublicKey.encoded,
            sequenceNumber = currentNumber,
            previousCounter = previousSendingChainLength.toInt(),
            ciphertext = ciphertext,
            iv = iv
        )
    }

    fun decrypt(message: RatchetMessage): ByteArray {
        val remotePublicKey = X25519PublicKeyParameters(message.ratchetPublicKey, 0)
        val remoteKeyB64 = java.util.Base64.getEncoder().encodeToString(message.ratchetPublicKey)

        // Check if message key was already skipped and stored
        val skippedKey = skippedMessageKeys.remove(Pair(remoteKeyB64, message.sequenceNumber))
        if (skippedKey != null) {
            return decryptWithKey(skippedKey, message.iv, message.ciphertext)
        }

        // Check if a DH ratchet step is needed
        if (dhReceivingPublicKey == null || !dhReceivingPublicKey!!.encoded.contentEquals(message.ratchetPublicKey)) {
            // Skip any unreceived messages in the current receiving chain
            skipMessageKeys(message.previousCounter.toLong())

            // DH Ratchet step
            val dhSecretReceive = computeDH(dhSendingPrivateKey, remotePublicKey)
            val kdfOutReceive = kdfRoot(rootKey, dhSecretReceive)
            rootKey = kdfOutReceive.first
            receivingChainKey = kdfOutReceive.second
            dhReceivingPublicKey = remotePublicKey
            receivingMessageNumber = 0

            // Generate new sending DH key pair
            val keyGen = X25519KeyPairGenerator()
            keyGen.init(X25519KeyGenerationParameters(secureRandom))
            val newPair = keyGen.generateKeyPair()
            dhSendingPrivateKey = newPair.private as X25519PrivateKeyParameters
            dhSendingPublicKey = newPair.public as X25519PublicKeyParameters

            val dhSecretSend = computeDH(dhSendingPrivateKey, remotePublicKey)
            val kdfOutSend = kdfRoot(rootKey, dhSecretSend)
            rootKey = kdfOutSend.first
            sendingChainKey = kdfOutSend.second
            previousSendingChainLength = sendingMessageNumber
            sendingMessageNumber = 0
        }

        // Skip any out-of-order messages in the receiving chain
        skipMessageKeys(message.sequenceNumber)

        // Derive message key
        val messageKey = kdfChainKey(receivingChainKey!!)
        receivingChainKey = kdfNextChainKey(receivingChainKey!!)
        receivingMessageNumber++

        return decryptWithKey(messageKey, message.iv, message.ciphertext)
    }

    private fun skipMessageKeys(until: Long) {
        if (receivingChainKey == null) return
        while (receivingMessageNumber < until) {
            val skippedKey = kdfChainKey(receivingChainKey!!)
            receivingChainKey = kdfNextChainKey(receivingChainKey!!)
            val remoteKeyB64 = java.util.Base64.getEncoder().encodeToString(dhReceivingPublicKey!!.encoded)
            skippedMessageKeys[Pair(remoteKeyB64, receivingMessageNumber)] = skippedKey
            receivingMessageNumber++
        }
    }

    private fun decryptWithKey(key: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        return cipher.doFinal(ciphertext)
    }

    private fun computeDH(privateKey: X25519PrivateKeyParameters, publicKey: X25519PublicKeyParameters): ByteArray {
        val agreement = X25519Agreement()
        agreement.init(privateKey)
        val secret = ByteArray(agreement.agreementSize)
        agreement.calculateAgreement(publicKey, secret, 0)
        return secret
    }

    private fun kdfRoot(rootKey: ByteArray, dhSecret: ByteArray): Pair<ByteArray, ByteArray> {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(rootKey, "HmacSHA256"))
        val prk = mac.doFinal(dhSecret)

        val mac1 = Mac.getInstance("HmacSHA256")
        mac1.init(SecretKeySpec(prk, "HmacSHA256"))
        mac1.update("PrivyChat-Root".toByteArray(Charsets.UTF_8))
        mac1.update(0x01.toByte())
        val nextRoot = mac1.doFinal()

        val mac2 = Mac.getInstance("HmacSHA256")
        mac2.init(SecretKeySpec(prk, "HmacSHA256"))
        mac2.update(nextRoot)
        mac2.update("PrivyChat-Chain".toByteArray(Charsets.UTF_8))
        mac2.update(0x02.toByte())
        val nextChain = mac2.doFinal()

        return Pair(nextRoot, nextChain)
    }

    private fun kdfChainKey(chainKey: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(chainKey, "HmacSHA256"))
        mac.update(0x01.toByte())
        return mac.doFinal()
    }

    private fun kdfNextChainKey(chainKey: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(chainKey, "HmacSHA256"))
        mac.update(0x02.toByte())
        return mac.doFinal()
    }
}

