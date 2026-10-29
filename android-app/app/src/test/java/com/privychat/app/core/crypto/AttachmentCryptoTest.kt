package com.privychat.app.core.crypto

import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom

class AttachmentCryptoTest {

    @Test
    fun testFileEncryptionAndDecryptionRoundtrip() {
        val originalData = "Zero-Knowledge Media Attachment Payload - PrivyChat".toByteArray(Charsets.UTF_8)

        val encrypted = AttachmentCrypto.encryptFile(originalData)

        assertNotNull(encrypted.ciphertext)
        assertNotNull(encrypted.key)
        assertNotNull(encrypted.iv)
        assertEquals(32, encrypted.key.size)
        assertEquals(12, encrypted.iv.size)
        assertEquals(64, encrypted.sha256Checksum.length)

        val decrypted = AttachmentCrypto.decryptFile(
            ciphertext = encrypted.ciphertext,
            key = encrypted.key,
            iv = encrypted.iv,
            expectedSha256 = encrypted.sha256Checksum
        )

        assertArrayEquals(originalData, decrypted)
    }

    @Test(expected = IllegalStateException::class)
    fun testAttachmentTamperingFailsIntegrityCheck() {
        val originalData = "Sensitive Document Content".toByteArray(Charsets.UTF_8)
        val encrypted = AttachmentCrypto.encryptFile(originalData)

        // Tamper with the ciphertext byte
        val tamperedCiphertext = encrypted.ciphertext.clone()
        tamperedCiphertext[0] = (tamperedCiphertext[0].toInt() xor 0xFF).toByte()

        // Expect IllegalStateException due to checksum failure
        AttachmentCrypto.decryptFile(
            ciphertext = tamperedCiphertext,
            key = encrypted.key,
            iv = encrypted.iv,
            expectedSha256 = encrypted.sha256Checksum
        )
    }
}

