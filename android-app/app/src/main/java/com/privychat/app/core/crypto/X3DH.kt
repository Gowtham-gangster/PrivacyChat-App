package com.privychat.app.core.crypto

import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.*
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object X3DH {

    private val secureRandom = SecureRandom()

    data class KeyBundle(
        val identityKeyPair: Pair<X25519PrivateKeyParameters, X25519PublicKeyParameters>,
        val signingKeyPair: Pair<Ed25519PrivateKeyParameters, Ed25519PublicKeyParameters>,
        val signedPreKeyPair: Pair<X25519PrivateKeyParameters, X25519PublicKeyParameters>,
        val signedPreKeySignature: ByteArray,
        val oneTimePreKeyPairs: List<Pair<X25519PrivateKeyParameters, X25519PublicKeyParameters>>
    )

    fun generateInitialKeyBundle(oneTimeKeysCount: Int = 50): KeyBundle {
        val x25519Gen = X25519KeyPairGenerator().apply {
            init(X25519KeyGenerationParameters(secureRandom))
        }
        val ed25519Gen = Ed25519KeyPairGenerator().apply {
            init(Ed25519KeyGenerationParameters(secureRandom))
        }

        // Identity keys
        val idPair = x25519Gen.generateKeyPair()
        val identityKeyPair = Pair(idPair.private as X25519PrivateKeyParameters, idPair.public as X25519PublicKeyParameters)

        val signPair = ed25519Gen.generateKeyPair()
        val signingKeyPair = Pair(signPair.private as Ed25519PrivateKeyParameters, signPair.public as Ed25519PublicKeyParameters)

        // Signed prekey
        val spkPair = x25519Gen.generateKeyPair()
        val signedPreKeyPair = Pair(spkPair.private as X25519PrivateKeyParameters, spkPair.public as X25519PublicKeyParameters)

        // Sign SPK public key with Ed25519 signing key
        val signer = Ed25519Signer()
        signer.init(true, signingKeyPair.first)
        val spkBytes = signedPreKeyPair.second.encoded
        signer.update(spkBytes, 0, spkBytes.size)
        val signature = signer.generateSignature()

        // One-time prekeys
        val otpkList = (1..oneTimeKeysCount).map {
            val p = x25519Gen.generateKeyPair()
            Pair(p.private as X25519PrivateKeyParameters, p.public as X25519PublicKeyParameters)
        }

        return KeyBundle(
            identityKeyPair = identityKeyPair,
            signingKeyPair = signingKeyPair,
            signedPreKeyPair = signedPreKeyPair,
            signedPreKeySignature = signature,
            oneTimePreKeyPairs = otpkList
        )
    }

    /**
     * Alice initiates X3DH with Bob's public keys.
     */
    fun aliceInitiate(
        aliceIdentityPrivateKey: X25519PrivateKeyParameters,
        bobIdentityPublicKey: X25519PublicKeyParameters,
        bobSignedPreKeyPublic: X25519PublicKeyParameters,
        bobOneTimePreKeyPublic: X25519PublicKeyParameters?
    ): Pair<ByteArray, Pair<X25519PrivateKeyParameters, X25519PublicKeyParameters>> {
        val x25519Gen = X25519KeyPairGenerator().apply {
            init(X25519KeyGenerationParameters(secureRandom))
        }
        val ephemPair = x25519Gen.generateKeyPair()
        val aliceEphemeralPrivate = ephemPair.private as X25519PrivateKeyParameters
        val aliceEphemeralPublic = ephemPair.public as X25519PublicKeyParameters

        val dh1 = computeDH(aliceIdentityPrivateKey, bobSignedPreKeyPublic)
        val dh2 = computeDH(aliceEphemeralPrivate, bobIdentityPublicKey)
        val dh3 = computeDH(aliceEphemeralPrivate, bobSignedPreKeyPublic)
        val dh4 = if (bobOneTimePreKeyPublic != null) computeDH(aliceEphemeralPrivate, bobOneTimePreKeyPublic) else ByteArray(0)

        val combinedDH = dh1 + dh2 + dh3 + dh4
        val masterSecret = deriveMasterKey(combinedDH)

        return Pair(masterSecret, Pair(aliceEphemeralPrivate, aliceEphemeralPublic))
    }

    /**
     * Bob responds to Alice's initial X3DH message.
     */
    fun bobReceive(
        bobIdentityPrivateKey: X25519PrivateKeyParameters,
        bobSignedPreKeyPrivate: X25519PrivateKeyParameters,
        bobOneTimePreKeyPrivate: X25519PrivateKeyParameters?,
        aliceIdentityPublicKey: X25519PublicKeyParameters,
        aliceEphemeralPublicKey: X25519PublicKeyParameters
    ): ByteArray {
        val dh1 = computeDH(bobSignedPreKeyPrivate, aliceIdentityPublicKey)
        val dh2 = computeDH(bobIdentityPrivateKey, aliceEphemeralPublicKey)
        val dh3 = computeDH(bobSignedPreKeyPrivate, aliceEphemeralPublicKey)
        val dh4 = if (bobOneTimePreKeyPrivate != null) computeDH(bobOneTimePreKeyPrivate, aliceEphemeralPublicKey) else ByteArray(0)

        val combinedDH = dh1 + dh2 + dh3 + dh4
        return deriveMasterKey(combinedDH)
    }

    private fun computeDH(priv: X25519PrivateKeyParameters, pub: X25519PublicKeyParameters): ByteArray {
        val agreement = X25519Agreement()
        agreement.init(priv)
        val secret = ByteArray(agreement.agreementSize)
        agreement.calculateAgreement(pub, secret, 0)
        return secret
    }

    private fun deriveMasterKey(ikm: ByteArray): ByteArray {
        val salt = "PrivyChat-X3DH-Salt-v1".toByteArray(Charsets.UTF_8)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = mac.doFinal(ikm)

        val macExpand = Mac.getInstance("HmacSHA256")
        macExpand.init(SecretKeySpec(prk, "HmacSHA256"))
        macExpand.update("PrivyChat-MasterSecret".toByteArray(Charsets.UTF_8))
        macExpand.update(0x01.toByte())
        return macExpand.doFinal()
    }
}

