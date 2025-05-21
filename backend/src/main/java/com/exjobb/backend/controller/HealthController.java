package com.exjobb.backend.controller;

import com.exjobb.backend.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple health check controller.
 * Provides basic system status information.
 */
@RestController
@RequestMapping("/api/health")
@Slf4j
public class HealthController {

    /**
     * Basic health check endpoint.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "marketing-agent-factory-backend");
        health.put("timestamp", LocalDateTime.now());
        health.put("version", "1.0.0");

        return ResponseEntity.ok(
                ApiResponse.success(health, "Service is running")
        );
    }
}