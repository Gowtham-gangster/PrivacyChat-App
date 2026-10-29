package com.privychat.app.core.crypto

import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.security.SecureRandom

class DoubleRatchetTest {

    private lateinit var random: SecureRandom

    @Before
    fun setUp() {
        random = SecureRandom()
    }

    @Test
    fun testAliceAndBobBidirectionalMessaging() {
        // Shared master root key from X3DH
        val sharedMasterSecret = ByteArray(32).also { random.nextBytes(it) }

        // Bob's pre-published ratchet key pair
        val x25519Gen = X25519KeyPairGenerator().apply {
            init(X25519KeyGenerationParameters(random))
        }
        val bobPair = x25519Gen.generateKeyPair()
        val bobPriv = bobPair.private as X25519PrivateKeyParameters
        val bobPub = bobPair.public as X25519PublicKeyParameters

        // Alice generates her initial ratchet key pair
        val alicePair = x25519Gen.generateKeyPair()
        val alicePriv = alicePair.private as X25519PrivateKeyParameters
        val alicePub = alicePair.public as X25519PublicKeyParameters

        // Alice initializes DoubleRatchet with Bob's public ratchet key
        val aliceRatchet = DoubleRatchet(
            rootKey = sharedMasterSecret.clone(),
            dhSendingPrivateKey = alicePriv,
            dhSendingPublicKey = alicePub,
            dhReceivingPublicKey = bobPub
        )

        // Bob initializes DoubleRatchet with his own key pair (receiving key null until Alice's first message)
        val bobRatchet = DoubleRatchet(
            rootKey = sharedMasterSecret.clone(),
            dhSendingPrivateKey = bobPriv,
            dhSendingPublicKey = bobPub,
            dhReceivingPublicKey = null
        )

        // 1. Alice sends message 1 to Bob
        val msg1Text = "Hello Bob! This is private."
        val msg1Encrypted = aliceRatchet.encrypt(msg1Text.toByteArray(Charsets.UTF_8))
        val msg1Decrypted = bobRatchet.decrypt(msg1Encrypted)
        assertEquals(msg1Text, String(msg1Decrypted, Charsets.UTF_8))

        // 2. Alice sends message 2 to Bob (same sending chain)
        val msg2Text = "Second message in sequence."
        val msg2Encrypted = aliceRatchet.encrypt(msg2Text.toByteArray(Charsets.UTF_8))
        val msg2Decrypted = bobRatchet.decrypt(msg2Encrypted)
        assertEquals(msg2Text, String(msg2Decrypted, Charsets.UTF_8))

        // 3. Bob replies to Alice (DH ratchet turns)
        val msg3Text = "Hey Alice! I got both messages."
        val msg3Encrypted = bobRatchet.encrypt(msg3Text.toByteArray(Charsets.UTF_8))
        val msg3Decrypted = aliceRatchet.decrypt(msg3Encrypted)
        assertEquals(msg3Text, String(msg3Decrypted, Charsets.UTF_8))

        // 4. Alice replies back to Bob (DH ratchet turns again)
        val msg4Text = "Ratchet advanced smoothly!"
        val msg4Encrypted = aliceRatchet.encrypt(msg4Text.toByteArray(Charsets.UTF_8))
        val msg4Decrypted = bobRatchet.decrypt(msg4Encrypted)
        assertEquals(msg4Text, String(msg4Decrypted, Charsets.UTF_8))
    }

    @Test
    fun testOutOfOrderMessageDelivery() {
        val sharedMasterSecret = ByteArray(32).also { random.nextBytes(it) }

        val x25519Gen = X25519KeyPairGenerator().apply {
            init(X25519KeyGenerationParameters(random))
        }
        val bobPair = x25519Gen.generateKeyPair()
        val bobPriv = bobPair.private as X25519PrivateKeyParameters
        val bobPub = bobPair.public as X25519PublicKeyParameters

        val alicePair = x25519Gen.generateKeyPair()
        val alicePriv = alicePair.private as X25519PrivateKeyParameters
        val alicePub = alicePair.public as X25519PublicKeyParameters

        val aliceRatchet = DoubleRatchet(
            rootKey = sharedMasterSecret.clone(),
            dhSendingPrivateKey = alicePriv,
            dhSendingPublicKey = alicePub,
            dhReceivingPublicKey = bobPub
        )

        val bobRatchet = DoubleRatchet(
            rootKey = sharedMasterSecret.clone(),
            dhSendingPrivateKey = bobPriv,
            dhSendingPublicKey = bobPub,
            dhReceivingPublicKey = null
        )

        val m1 = aliceRatchet.encrypt("Packet 1".toByteArray(Charsets.UTF_8))
        val m2 = aliceRatchet.encrypt("Packet 2".toByteArray(Charsets.UTF_8))
        val m3 = aliceRatchet.encrypt("Packet 3".toByteArray(Charsets.UTF_8))

        // Deliver m3 first!
        val d3 = bobRatchet.decrypt(m3)
        assertEquals("Packet 3", String(d3, Charsets.UTF_8))

        // Deliver m1 second!
        val d1 = bobRatchet.decrypt(m1)
        assertEquals("Packet 1", String(d1, Charsets.UTF_8))

        // Deliver m2 last!
        val d2 = bobRatchet.decrypt(m2)
        assertEquals("Packet 2", String(d2, Charsets.UTF_8))
    }
}

