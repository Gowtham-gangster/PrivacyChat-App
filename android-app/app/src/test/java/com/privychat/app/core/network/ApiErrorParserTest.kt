package com.privychat.app.core.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class ApiErrorParserTest {

    @Test
    fun testParseSpringValidationErrorDetails() {
        val json = """
            {
                "timestamp": "2026-11-01T06:25:21Z",
                "status": 400,
                "error": "Validation Failed",
                "details": {
                    "phoneNumber": "Phone number must be valid E.164 format (e.g. +12025550143)"
                }
            }
        """.trimIndent()

        val response = Response.error<Any>(400, json.toResponseBody("application/json".toMediaType()))
        val parsed = ApiErrorParser.parseError(response, "Registration failed")

        assertEquals("Phone number must be valid E.164 format (e.g. +12025550143)", parsed)
    }

    @Test
    fun testParseBadRequestMessage() {
        val json = """
            {
                "timestamp": "2026-11-01T06:25:21Z",
                "status": 400,
                "error": "Bad Request",
                "message": "An account with this email address already exists"
            }
        """.trimIndent()

        val response = Response.error<Any>(400, json.toResponseBody("application/json".toMediaType()))
        val parsed = ApiErrorParser.parseError(response, "Registration failed")

        assertEquals("An account with this email address already exists", parsed)
    }

    @Test
    fun testFallbackOnEmptyBody() {
        val response = Response.error<Any>(400, "".toResponseBody("application/json".toMediaType()))
        val parsed = ApiErrorParser.parseError(response, "Registration failed")

        assertEquals("Registration failed (400)", parsed)
    }
}

