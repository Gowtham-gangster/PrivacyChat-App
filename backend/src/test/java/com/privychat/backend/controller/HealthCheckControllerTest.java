package com.privychat.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class HealthCheckControllerTest {

    private final HealthCheckController controller = new HealthCheckController();

    @Test
    void testHealthEndpointReturnsUpStatus() {
        ResponseEntity<Map<String, Object>> response = controller.health();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("privychat-backend", response.getBody().get("application"));
    }
}

