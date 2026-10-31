package com.privychat.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health check controller providing immediate 200 OK responses for ALB (Application Load Balancer),
 * Elastic Beanstalk health monitors, and container orchestrator probes on both root (/) and /health.
 */
@RestController
public class HealthCheckController {

    @GetMapping({"/", "/health"})
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "application", "privychat-backend"
        ));
    }
}

