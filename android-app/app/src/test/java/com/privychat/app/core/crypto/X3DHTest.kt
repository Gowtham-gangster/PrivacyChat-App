package com.privychat.app.core.crypto

import org.bouncycastle.crypto.signers.Ed25519Signer
import org.junit.Assert.*
import org.junit.Test

class X3DHTest {

    @Test
    fun testKeyBundleGenerationAndSignatureVerification() {
        val bundle = X3DH.generateInitialKeyBundle(oneTimeKeysCount = 10)

        assertNotNull(bundle.identityKeyPair)
        assertNotNull(bundle.signingKeyPair)
        assertNotNull(bundle.signedPreKeyPair)
        assertEquals(10, bundle.oneTimePreKeyPairs.size)

        // Verify Ed25519 signature of the Signed PreKey
        val verifier = Ed25519Signer()
        verifier.init(false, bundle.signingKeyPair.second)
        val spkBytes = bundle.signedPreKeyPair.second.encoded
        verifier.update(spkBytes, 0, spkBytes.size)
        val isValid = verifier.verifySignature(bundle.signedPreKeySignature)

        assertTrue("Ed25519 signature on Signed PreKey must be valid", isValid)
    }

    @Test
    fun testAliceAndBobDeriveIdenticalMasterSecretWithOneTimePreKey() {
        val aliceBundle = X3DH.generateInitialKeyBundle(oneTimeKeysCount = 5)
        val bobBundle = X3DH.generateInitialKeyBundle(oneTimeKeysCount = 5)

        val bobOtpkPair = bobBundle.oneTimePreKeyPairs[0]

        // Alice initiates handshake
        val (aliceMasterSecret, aliceEphemeralPair) = X3DH.aliceInitiate(
            aliceIdentityPrivateKey = aliceBundle.identityKeyPair.first,
            bobIdentityPublicKey = bobBundle.identityKeyPair.second,
            bobSignedPreKeyPublic = bobBundle.signedPreKeyPair.second,
            bobOneTimePreKeyPublic = bobOtpkPair.second
        )

        // Bob responds to Alice
        val bobMasterSecret = X3DH.bobReceive(
            bobIdentityPrivateKey = bobBundle.identityKeyPair.first,
            bobSignedPreKeyPrivate = bobBundle.signedPreKeyPair.first,
            bobOneTimePreKeyPrivate = bobOtpkPair.first,
            aliceIdentityPublicKey = aliceBundle.identityKeyPair.second,
            aliceEphemeralPublicKey = aliceEphemeralPair.second
        )

        assertEquals(32, aliceMasterSecret.size)
        assertEquals(32, bobMasterSecret.size)
        assertArrayEquals("Alice and Bob must derive identical master secret", aliceMasterSecret, bobMasterSecret)
    }

    @Test
    fun testAliceAndBobDeriveIdenticalMasterSecretWithoutOneTimePreKey() {
        val aliceBundle = X3DH.generateInitialKeyBundle(oneTimeKeysCount = 1)
        val bobBundle = X3DH.generateInitialKeyBundle(oneTimeKeysCount = 1)

        // Alice initiates handshake when Bob has no OTPKs remaining (null)
        val (aliceMasterSecret, aliceEphemeralPair) = X3DH.aliceInitiate(
            aliceIdentityPrivateKey = aliceBundle.identityKeyPair.first,
            bobIdentityPublicKey = bobBundle.identityKeyPair.second,
            bobSignedPreKeyPublic = bobBundle.signedPreKeyPair.second,
            bobOneTimePreKeyPublic = null
        )

        // Bob responds without OTPK
        val bobMasterSecret = X3DH.bobReceive(
            bobIdentityPrivateKey = bobBundle.identityKeyPair.first,
            bobSignedPreKeyPrivate = bobBundle.signedPreKeyPair.first,
            bobOneTimePreKeyPrivate = null,
            aliceIdentityPublicKey = aliceBundle.identityKeyPair.second,
            aliceEphemeralPublicKey = aliceEphemeralPair.second
        )

        assertEquals(32, aliceMasterSecret.size)
        assertEquals(32, bobMasterSecret.size)
        assertArrayEquals("Alice and Bob must derive identical master secret without OTPK", aliceMasterSecret, bobMasterSecret)
    }
}

