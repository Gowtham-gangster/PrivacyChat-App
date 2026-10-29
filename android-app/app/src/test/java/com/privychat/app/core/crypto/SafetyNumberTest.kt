package com.privychat.app.core.crypto

import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom

class SafetyNumberTest {

    @Test
    fun testSafetyNumberOrderInvariance() {
        val random = SecureRandom()
        val aliceIdentityKey = ByteArray(32).also { random.nextBytes(it) }
        val bobIdentityKey = ByteArray(32).also { random.nextBytes(it) }

        val aliceView = SafetyNumber.generateSafetyNumber(aliceIdentityKey, bobIdentityKey)
        val bobView = SafetyNumber.generateSafetyNumber(bobIdentityKey, aliceIdentityKey)

        assertEquals("Safety number must be commutative/order invariant", aliceView, bobView)

        // Verify format: 12 blocks of 5 digits separated by spaces
        val blocks = aliceView.split(" ")
        assertEquals(12, blocks.size)
        for (block in blocks) {
            assertEquals(5, block.length)
            assertTrue(block.all { it.isDigit() })
        }
    }

    @Test
    fun testDifferentKeysYieldDifferentSafetyNumbers() {
        val random = SecureRandom()
        val aliceKey = ByteArray(32).also { random.nextBytes(it) }
        val bobKey = ByteArray(32).also { random.nextBytes(it) }
        val eveKey = ByteArray(32).also { random.nextBytes(it) }

        val legitimateNumber = SafetyNumber.generateSafetyNumber(aliceKey, bobKey)
        val mitmNumber = SafetyNumber.generateSafetyNumber(aliceKey, eveKey)

        assertNotEquals(legitimateNumber, mitmNumber)
    }

    @Test
    fun testQrPayloadGeneration() {
        val random = SecureRandom()
        val aliceKey = ByteArray(32).also { random.nextBytes(it) }
        val bobKey = ByteArray(32).also { random.nextBytes(it) }

        val qrPayload = SafetyNumber.generateQrPayload(aliceKey, bobKey)

        assertTrue(qrPayload.startsWith("privychat:safety-number?v=1&num="))
        val digits = qrPayload.substringAfter("num=")
        assertEquals(60, digits.length)
        assertTrue(digits.all { it.isDigit() })
    }
}

