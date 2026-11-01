package com.privychat.app.core.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test

class RegistrationSerializationTest {

    private val gson = Gson()

    @Test
    fun testRegisterRequestDto_serializesToExactBackendPropertyNames() {
        val dto = RegisterRequestDto(
            displayName = "Test User",
            email = "user@example.com",
            phoneNumber = "+919705067505",
            password = "SecurePassword123!",
            termsAccepted = true
        )

        val json = gson.toJson(dto)
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val map: Map<String, Any> = gson.fromJson(json, type)

        // Verify all 5 expected backend property names are present
        assertTrue("JSON must contain 'displayName'", map.containsKey("displayName"))
        assertTrue("JSON must contain 'email'", map.containsKey("email"))
        assertTrue("JSON must contain 'phoneNumber'", map.containsKey("phoneNumber"))
        assertTrue("JSON must contain 'password'", map.containsKey("password"))
        assertTrue("JSON must contain 'termsAccepted'", map.containsKey("termsAccepted"))

        // Verify values
        assertEquals("Test User", map["displayName"])
        assertEquals("user@example.com", map["email"])
        assertEquals("+919705067505", map["phoneNumber"])
        assertEquals("SecurePassword123!", map["password"])
        assertEquals(true, map["termsAccepted"])

        // Verify total keys count is exactly 5 (no obfuscated keys like 'a', 'b', etc.)
        assertEquals(5, map.keys.size)
    }

    @Test
    fun testLoginRequestDto_serializesToExactBackendPropertyNames() {
        val dto = LoginRequestDto(
            identifier = "user@example.com",
            password = "SecurePassword123!",
            deviceLabel = "Pixel 8"
        )

        val json = gson.toJson(dto)
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val map: Map<String, Any> = gson.fromJson(json, type)

        assertTrue("JSON must contain 'identifier'", map.containsKey("identifier"))
        assertTrue("JSON must contain 'password'", map.containsKey("password"))
        assertTrue("JSON must contain 'deviceLabel'", map.containsKey("deviceLabel"))
        assertEquals(3, map.keys.size)
    }
}

