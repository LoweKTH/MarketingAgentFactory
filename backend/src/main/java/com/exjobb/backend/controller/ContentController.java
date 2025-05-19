package com.exjobb.backend.controller;

import com.exjobb.backend.dto.ApiResponse;
import com.exjobb.backend.dto.ContentGenerationRequest;
import com.exjobb.backend.dto.ContentGenerationResponse;
import com.exjobb.backend.entity.Task;
import com.exjobb.backend.entity.User;
import com.exjobb.backend.service.TaskRouterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for content generation operations.
 *
 * This controller serves as the main API gateway for content generation
 * requests from the frontend. It handles:
 * - Synchronous content generation
 * - Asynchronous content generation
 * - Task status checking
 * - Content generation history
 *
 * All endpoints require authentication and return standardized API responses.
 */
@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Content Generation", description = "Content generation and management operations")
@SecurityRequirement(name = "basicAuth")
public class ContentController {

    private final TaskRouterService taskRouterService;

    /**
     * Generate marketing content synchronously.
     *
     * This endpoint processes content generation requests synchronously,
     * meaning the client waits for the complete response. Best for:
     * - Quick content generation (< 30 seconds)
     * - Simple UI flows
     * - Testing and development
     *
     * @param request The content generation request
     * @param authentication The authenticated user
     * @return Generated content with metadata
     */
    @PostMapping("/generate")
    @Operation(
            summary = "Generate marketing content",
            description = "Generate marketing content based on the provided specifications. " +
                    "This is a synchronous operation that returns the generated content immediately."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Content generated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Content generation failed"
            )
    })
    public ResponseEntity<ApiResponse<ContentGenerationResponse>> generateContent(
            @Parameter(description = "Content generation specifications", required = true)
            @Valid @RequestBody ContentGenerationRequest request,
            Authentication authentication) {

        // Log the incoming request for monitoring
        log.info("Content generation request from user: {} for content type: {}",
                authentication.getName(), request.getContentType());

        try {
            // Get the authenticated user
            User currentUser = (User) authentication.getPrincipal();

            // Process the content generation request
            ContentGenerationResponse response = taskRouterService.processContentGeneration(request, currentUser);

            // Log successful completion
            log.info("Content generation completed successfully for user: {} task: {}",
                    currentUser.getUsername(), response.getTaskId());

            // Return successful response
            return ResponseEntity.ok(
                    ApiResponse.success(response, "Content generated successfully")
            );

        } catch (Exception e) {
            // Log the error (detailed logging is handled by GlobalExceptionHandler)
            log.error("Content generation failed for user: {}", authentication.getName(), e);

            // Exception will be handled by GlobalExceptionHandler
            throw e;
        }
    }

    /**
     * Generate marketing content asynchronously.
     *
     * This endpoint starts content generation in the background and
     * immediately returns a task ID. Best for:
     * - Long-running content generation
     * - Complex workflows
     * - UI with progress indicators
     *
     * @param request The content generation request
     * @param authentication The authenticated user
     * @return Task ID for tracking progress
     */
    @PostMapping("/generate/async")
    @Operation(
            summary = "Generate marketing content asynchronously",
            description = "Start content generation in the background and return a task ID " +
                    "for tracking progress. Use the task status endpoint to check completion."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "Content generation started successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
    public ResponseEntity<ApiResponse<TaskResponse>> generateContentAsync(
            @Parameter(description = "Content generation specifications", required = true)
            @Valid @RequestBody ContentGenerationRequest request,
            Authentication authentication) {

        // Log the async request
        log.info("Async content generation request from user: {} for content type: {}",
                authentication.getName(), request.getContentType());

        try {
            // Get the authenticated user
            User currentUser = (User) authentication.getPrincipal();

            // Start asynchronous processing
            String taskId = taskRouterService.processContentGenerationAsync(request, currentUser);

            // Create task response
            TaskResponse taskResponse = TaskResponse.builder()
                    .taskId(taskId)
                    .status("PROCESSING")
                    .message("Content generation started")
                    .contentType(request.getContentType())
                    .progress(0)
                    .currentStep("initializing")
                    .stepDescription("Initializing content generation")
                    .streamUrl("/api/content/tasks/" + taskId + "/stream")
                    .createdBy(currentUser.getUsername())
                    .createdAt(java.time.LocalDateTime.now())
                    .build();

            log.info("Async content generation started for user: {} task: {}",
                    currentUser.getUsername(), taskId);

            // Return 202 Accepted with task information
            return ResponseEntity.accepted()
                    .body(ApiResponse.success(taskResponse, "Content generation started"));

        } catch (Exception e) {
            log.error("Failed to start async content generation for user: {}", authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Get the status of a content generation task.
     *
     * This endpoint allows clients to check the progress and status
     * of both synchronous and asynchronous content generation tasks.
     *
     * @param taskId The unique task identifier
     * @param authentication The authenticated user
     * @return Current task status and results if completed
     */
    @GetMapping("/tasks/{taskId}")
    @Operation(
            summary = "Get task status",
            description = "Retrieve the current status and progress of a content generation task. " +
                    "Includes the generated content if the task is completed."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Task status retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Task not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Access denied - not your task"
            )
    })
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskStatus(
            @Parameter(description = "Task identifier", required = true)
            @PathVariable String taskId,
            Authentication authentication) {

        log.debug("Task status request for task: {} from user: {}", taskId, authentication.getName());

        try {
            // Get the authenticated user
            User currentUser = (User) authentication.getPrincipal();

            // Get task status from service
            Task task = taskRouterService.getTaskStatus(taskId);

            // Check if user has access to this task (users can only see their own tasks)
            if (!task.getCreatedBy().getId().equals(currentUser.getId())
                    && !currentUser.getRole().equals(User.Role.ADMIN)) {
                log.warn("User {} attempted to access task {} belonging to user {}",
                        currentUser.getUsername(), taskId, task.getCreatedBy().getUsername());

                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Access denied", "ACCESS_DENIED"));
            }

            // Convert task entity to response DTO
            TaskResponse taskResponse = convertTaskToResponse(task);

            log.debug("Returning task status for task: {} status: {}", taskId, task.getStatus());

            return ResponseEntity.ok(
                    ApiResponse.success(taskResponse, "Task status retrieved successfully")
            );

        } catch (IllegalArgumentException e) {
            // Task not found
            log.warn("Task not found: {} requested by user: {}", taskId, authentication.getName());
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Task not found", "TASK_NOT_FOUND"));

        } catch (Exception e) {
            log.error("Failed to get task status for task: {} user: {}", taskId, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Get content generation history for the authenticated user.
     *
     * This endpoint returns a paginated list of past content generation
     * tasks for the authenticated user.
     *
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @param authentication The authenticated user
     * @return List of user's content generation history
     */
    @GetMapping("/history")
    @Operation(
            summary = "Get content generation history",
            description = "Retrieve the authenticated user's content generation history " +
                    "with pagination support."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "History retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
    public ResponseEntity<ApiResponse<java.util.List<TaskResponse>>> getContentHistory(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        log.debug("Content history request from user: {} page: {} size: {}",
                authentication.getName(), page, size);

        try {
            // Get the authenticated user
            User currentUser = (User) authentication.getPrincipal();

            // Get user's tasks (simplified - in production would use pagination)
            var tasks = taskRouterService.getUserTasks(currentUser, page, size);

            // Convert tasks to response DTOs
            java.util.List<TaskResponse> taskResponses = tasks.stream()
                    .map(this::convertTaskToResponse)
                    .collect(java.util.stream.Collectors.toList());

            log.debug("Returning {} tasks for user: {}", taskResponses.size(), currentUser.getUsername());

            return ResponseEntity.ok(
                    ApiResponse.success(taskResponses, "Content history retrieved successfully")
            );

        } catch (Exception e) {
            log.error("Failed to get content history for user: {}", authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Cancel a pending or running content generation task.
     *
     * This endpoint allows users to cancel their own tasks that are
     * still in progress.
     *
     * @param taskId The task identifier
     * @param authentication The authenticated user
     * @return Confirmation of cancellation
     */
    @DeleteMapping("/tasks/{taskId}")
    @Operation(
            summary = "Cancel content generation task",
            description = "Cancel a pending or running content generation task. " +
                    "Only the task owner or an admin can cancel a task."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Task cancelled successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Task not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Access denied"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Task cannot be cancelled"
            )
    })
    public ResponseEntity<ApiResponse<Void>> cancelTask(
            @Parameter(description = "Task identifier", required = true)
            @PathVariable String taskId,
            Authentication authentication) {

        log.info("Task cancellation request for task: {} from user: {}", taskId, authentication.getName());

        try {
            // Get the authenticated user
            User currentUser = (User) authentication.getPrincipal();

            // Cancel the task (service will handle authorization and status checks)
            taskRouterService.cancelTask(taskId, currentUser);

            log.info("Task cancelled successfully: {} by user: {}", taskId, currentUser.getUsername());

            return ResponseEntity.ok(
                    ApiResponse.success("Task cancelled successfully")
            );

        } catch (IllegalArgumentException e) {
            log.warn("Task not found for cancellation: {} user: {}", taskId, authentication.getName());
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Task not found", "TASK_NOT_FOUND"));

        } catch (IllegalStateException e) {
            log.warn("Task cannot be cancelled: {} user: {} reason: {}",
                    taskId, authentication.getName(), e.getMessage());
            return ResponseEntity.status(409)
                    .body(ApiResponse.error("Task cannot be cancelled", "TASK_NOT_CANCELLABLE"));

        } catch (Exception e) {
            log.error("Failed to cancel task: {} user: {}", taskId, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Convert Task entity to TaskResponse DTO.
     *
     * This helper method transforms the internal Task entity into
     * a response DTO suitable for the frontend.
     *
     * @param task The task entity
     * @return Task response DTO
     */
    private TaskResponse convertTaskToResponse(Task task) {
        // Calculate progress based on status
        int progress = switch (task.getStatus()) {
            case CREATED -> 0;
            case PROCESSING -> 50;
            case COMPLETED -> 100;
            case FAILED -> 0;
        };

        // Determine if task can be retried
        boolean retryable = task.getStatus() == Task.TaskStatus.FAILED;

        return TaskResponse.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus().name())
                .message(getStatusMessage(task.getStatus()))
                .contentType(task.getContentType())
                .progress(progress)
                .currentStep(getCurrentStep(task.getStatus()))
                .stepDescription(getStepDescription(task.getStatus()))
                .streamUrl("/api/content/tasks/" + task.getTaskId() + "/stream")
                .errorMessage(task.getErrorMessage())
                .retryable(retryable)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .createdBy(task.getCreatedBy().getUsername())
                .build();
    }

    /**
     * Get human-readable status message for a task status.
     *
     * @param status The task status
     * @return Human-readable message
     */
    private String getStatusMessage(Task.TaskStatus status) {
        return switch (status) {
            case CREATED -> "Task created and queued for processing";
            case PROCESSING -> "Content generation in progress";
            case COMPLETED -> "Content generation completed successfully";
            case FAILED -> "Content generation failed";
        };
    }

    /**
     * Get current step name for a task status.
     *
     * @param status The task status
     * @return Current step name
     */
    private String getCurrentStep(Task.TaskStatus status) {
        return switch (status) {
            case CREATED -> "queued";
            case PROCESSING -> "generating";
            case COMPLETED -> "completed";
            case FAILED -> "failed";
        };
    }

    /**
     * Get step description for a task status.
     *
     * @param status The task status
     * @return Step description
     */
    private String getStepDescription(Task.TaskStatus status) {
        return switch (status) {
            case CREATED -> "Task is queued and waiting to be processed";
            case PROCESSING -> "AI is generating your content";
            case COMPLETED -> "Content generation finished successfully";
            case FAILED -> "Content generation encountered an error";
        };
    }
}