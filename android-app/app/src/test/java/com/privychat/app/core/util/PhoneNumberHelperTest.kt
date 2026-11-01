package com.privychat.app.core.util

import org.junit.Assert.*
import org.junit.Test

class PhoneNumberHelperTest {

    @Test
    fun testIndianMobileNumberWithoutPrefix_normalizesToE164() {
        val result = PhoneNumberHelper.formatToE164("9705067505")
        assertEquals("+919705067505", result)
        assertTrue(PhoneNumberHelper.isValidE164(result))
    }

    @Test
    fun testIndianMobileWithLeadingZero_normalizesToE164() {
        val result = PhoneNumberHelper.formatToE164("09705067505")
        assertEquals("+919705067505", result)
        assertTrue(PhoneNumberHelper.isValidE164(result))
    }

    @Test
    fun testIndianMobileWithCountryCodeWithoutPlus_normalizesToE164() {
        val result = PhoneNumberHelper.formatToE164("919705067505")
        assertEquals("+919705067505", result)
        assertTrue(PhoneNumberHelper.isValidE164(result))
    }

    @Test
    fun testIndianMobileWithSpacesAndPlus_normalizesToE164() {
        val result = PhoneNumberHelper.formatToE164("+91 97050 67505")
        assertEquals("+919705067505", result)
        assertTrue(PhoneNumberHelper.isValidE164(result))
    }

    @Test
    fun testNorthAmericanNumberWithPlus_normalizesToE164() {
        val result = PhoneNumberHelper.formatToE164("+12025550143")
        assertEquals("+12025550143", result)
        assertTrue(PhoneNumberHelper.isValidE164(result))
    }

    @Test
    fun testNorthAmericanNumberWithoutPlus_normalizesToE164() {
        val result = PhoneNumberHelper.formatToE164("12025550143")
        assertEquals("+12025550143", result)
        assertTrue(PhoneNumberHelper.isValidE164(result))
    }

    @Test
    fun testInternationalExitCode00_normalizesToPlus() {
        val result = PhoneNumberHelper.formatToE164("00919705067505")
        assertEquals("+919705067505", result)
        assertTrue(PhoneNumberHelper.isValidE164(result))
    }

    @Test
    fun testInvalidPhone_failsValidation() {
        assertFalse(PhoneNumberHelper.isValidE164("invalid_phone"))
        assertFalse(PhoneNumberHelper.isValidE164(""))
        assertFalse(PhoneNumberHelper.isValidE164("+012345"))
    }
}

