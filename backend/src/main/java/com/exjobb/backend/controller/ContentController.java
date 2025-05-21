package com.exjobb.backend.controller;

import com.exjobb.backend.dto.ApiResponse;
import com.exjobb.backend.dto.ContentGenerationRequest;
import com.exjobb.backend.dto.ContentGenerationResponse;
import com.exjobb.backend.entity.User;
import com.exjobb.backend.service.TaskRouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Simple REST controller for content generation.
 * Focuses on basic functionality with the evaluator-optimizer workflow.
 */
@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173") // Allow frontend origin
public class ContentController {

    private final TaskRouterService taskRouterService;

    /**
     * Generate marketing content synchronously.
     * Uses the evaluator-optimizer workflow from the Python service.
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ContentGenerationResponse>> generateContent(
            @RequestBody ContentGenerationRequest request) {

        System.out.println("*******************************");
        System.out.println("ENDPOINT REACHED! Request: " + request);
        System.out.println("*******************************");

        log.info("ENDPOINT REACHED! Content type: {}", request.getContentType());

        try {
            // For now, create a dummy user until authentication is working
            User dummyUser = User.builder()
                    .id(1L)
                    .username("testuser")
                    .email("test@example.com")
                    .build();

            // Process the content generation request
            ContentGenerationResponse response = taskRouterService.processContentGeneration(request, dummyUser);

            log.info("Content generation completed successfully");

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Content generated successfully")
            );

        } catch (Exception e) {
            log.error("Content generation failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Content generation failed", e.getMessage()));
        }
    }

    /**
     * Simple health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        System.out.println("HEALTH ENDPOINT REACHED");
        return ResponseEntity.ok("Content service is running");
    }
}