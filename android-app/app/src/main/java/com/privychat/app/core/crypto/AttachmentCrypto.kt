package com.privychat.app.core.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.HexFormat
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AttachmentCrypto {

    private val secureRandom = SecureRandom()

    data class EncryptedPayload(
        val ciphertext: ByteArray,
        val key: ByteArray,
        val iv: ByteArray,
        val sha256Checksum: String
    )

    fun encryptFile(plaintext: ByteArray): EncryptedPayload {
        val key = ByteArray(32).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val ciphertext = cipher.doFinal(plaintext)

        val digest = MessageDigest.getInstance("SHA-256").digest(ciphertext)
        val checksum = HexFormat.of().formatHex(digest)

        return EncryptedPayload(
            ciphertext = ciphertext,
            key = key,
            iv = iv,
            sha256Checksum = checksum
        )
    }

    fun decryptFile(ciphertext: ByteArray, key: ByteArray, iv: ByteArray, expectedSha256: String?): ByteArray {
        if (expectedSha256 != null) {
            val digest = MessageDigest.getInstance("SHA-256").digest(ciphertext)
            val actualChecksum = HexFormat.of().formatHex(digest)
            if (!actualChecksum.equals(expectedSha256, ignoreCase = true)) {
                throw IllegalStateException("Attachment integrity check failed: SHA-256 mismatch")
            }
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        return cipher.doFinal(ciphertext)
    }
}

