package com.exjobb.backend.controller;

import com.exjobb.backend.dto.ApiResponse;
import com.exjobb.backend.dto.ContentGenerationRequest;
import com.exjobb.backend.dto.ContentGenerationResponse;
import com.exjobb.backend.dto.SaveContentRequest;
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
@CrossOrigin(origins = "http://localhost:5173")
public class ContentController {

    private final TaskRouterService taskRouterService;

    /**
     * Generate content WITHOUT saving to database
     * Returns the generated content for user preview
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ContentGenerationResponse>> generateContent(
            @RequestBody ContentGenerationRequest request) {

        log.info("Generating content for type: {}", request.getContentType());

        try {
            User dummyUser = User.builder()
                    .id(1L)
                    .username("testuser")
                    .email("test@example.com")
                    .build();

            // Only generate content, don't save to database yet
            ContentGenerationResponse response = taskRouterService.generateContentOnly(request, dummyUser);

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
     * Save the generated content to database
     * Called when user clicks "Save" button
     */
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<String>> saveContent(
            @RequestBody SaveContentRequest saveRequest) {

        log.info("Saving content to database");

        try {
            User dummyUser = User.builder()
                    .id(1L)
                    .username("testuser")
                    .email("test@example.com")
                    .build();

            String taskId = taskRouterService.saveGeneratedContent(saveRequest, dummyUser);

            return ResponseEntity.ok(
                    ApiResponse.success(taskId, "Content saved successfully")
            );

        } catch (Exception e) {
            log.error("Content save failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Content save failed", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Content service is running");
    }
}