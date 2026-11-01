package com.privychat.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.privychat.backend.dto.auth.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class RegisterRequestValidationTest {

    private Validator validator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    @DisplayName("Valid RegisterRequest passes all Jakarta validation rules")
    void testValidRegisterRequest() {
        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "+919705067505", "Password123!", true);
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "Valid request should have 0 violations");
    }

    @Test
    @DisplayName("Empty or null fields trigger all 4 required field violations")
    void testEmptyFieldsTriggerExpectedViolations() {
        RegisterRequest req = new RegisterRequest(null, null, null, null, false);
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);

        Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertTrue(messages.contains("Display name is required"));
        assertTrue(messages.contains("Email is required"));
        assertTrue(messages.contains("Phone number is required"));
        assertTrue(messages.contains("Password is required"));
    }

    @Test
    @DisplayName("Obfuscated JSON (e.g. from R8/ProGuard without @SerializedName or keep rules) results in null fields")
    void testObfuscatedJsonFailsValidationWithAllRequiredFieldErrors() throws Exception {
        // When R8 obfuscates fields to a, b, c, d
        String obfuscatedJson = "{\"a\":\"Alice\",\"b\":\"alice@example.com\",\"c\":\"+919705067505\",\"d\":\"Password123!\",\"e\":true}";
        RegisterRequest req = objectMapper.readValue(obfuscatedJson, RegisterRequest.class);

        assertNull(req.getDisplayName());
        assertNull(req.getEmail());
        assertNull(req.getPhoneNumber());
        assertNull(req.getPassword());

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertEquals(4, violations.size());
        assertTrue(messages.contains("Display name is required"));
        assertTrue(messages.contains("Email is required"));
        assertTrue(messages.contains("Phone number is required"));
        assertTrue(messages.contains("Password is required"));
    }

    @Test
    @DisplayName("Properly named JSON deserializes and validates successfully")
    void testProperJsonValidatesSuccessfully() throws Exception {
        String json = "{\"displayName\":\"Alice\",\"email\":\"alice@example.com\",\"phoneNumber\":\"+919705067505\",\"password\":\"Password123!\",\"termsAccepted\":true}";
        RegisterRequest req = objectMapper.readValue(json, RegisterRequest.class);

        assertEquals("Alice", req.getDisplayName());
        assertEquals("alice@example.com", req.getEmail());
        assertEquals("+919705067505", req.getPhoneNumber());
        assertEquals("Password123!", req.getPassword());
        assertTrue(req.isTermsAccepted());

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }
}
