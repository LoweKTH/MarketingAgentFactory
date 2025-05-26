package com.exjobb.backend.service;

import com.exjobb.backend.dto.ContentGenerationRequest;
import com.exjobb.backend.dto.ContentGenerationResponse;
import com.exjobb.backend.dto.SaveContentRequest;
import com.exjobb.backend.dto.TaskDto;
import com.exjobb.backend.entity.Task;
import com.exjobb.backend.entity.User;
import com.exjobb.backend.entity.BrandGuideline;
import com.exjobb.backend.repository.TaskRepository;
import com.exjobb.backend.utils.TaskMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Task Router Service - The central coordinator for content generation.
 *
 * This service acts as the "Simple Task Router" from our architecture diagram.
 * It coordinates between the frontend, database, and Python Content Agent.
 *
 * Key responsibilities:
 * - Create and manage tasks
 * - Load brand guidelines and context
 * - Coordinate with Python Content Agent
 * - Store results and manage task lifecycle
 * - Implement evaluator-optimizer workflow
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskRouterService {

    private final TaskRepository taskRepository;
    private final PythonServiceClient pythonServiceClient;
    private final UserService userService;
    // TODO: Add BrandGuidelineService when implemented
    // private final BrandGuidelineService brandGuidelineService;

    /**
     * Process a content generation request synchronously.
     *
     * This is the main entry point for content generation. It:
     * 1. Creates a new task in the database
     * 2. Loads relevant brand guidelines
     * 3. Calls the Python Content Agent with evaluator-optimizer workflow
     * 4. Stores the results
     * 5. Returns the generated content
     *
     * @param request The content generation request from the frontend
     * @param currentUser The user making the request
     * @return The generated content and metadata
     */
    @Transactional
    public ContentGenerationResponse processContentGeneration(ContentGenerationRequest request, User currentUser) {
        log.info("Processing content generation request for user: {} content type: {}",
                currentUser.getUsername(), request.getContentType());

        try {
            // Step 1: Create and save the task
            Task task = createTask(request, currentUser);
            log.debug("Created task with ID: {}", task.getTaskId());

            // Step 2: Load brand guidelines (for now using default/empty)
            BrandGuideline brandGuidelines = getDefaultBrandGuidelines();
            log.debug("Using default brand guidelines");

            // Step 3: Update task status to PROCESSING
            task.setStatus(Task.TaskStatus.PROCESSING);
            task.setUpdatedAt(LocalDateTime.now());
            task = taskRepository.save(task);

            // Step 4: Prepare request for Python service
            var pythonRequest = buildPythonRequest(request, brandGuidelines);

            // Step 5: Call Python Content Agent (includes evaluator-optimizer workflow)
            log.info("Calling Python Content Agent for task: {}", task.getTaskId());
            var pythonResponse = pythonServiceClient.generateContent(pythonRequest);

            // Step 6: Process the response
            ContentGenerationResponse response = processSuccessfulResponse(task, pythonResponse);

            // Step 7: Update task with results
            updateTaskWithSuccess(task, pythonResponse);

            log.info("Successfully completed content generation for task: {}", task.getTaskId());
            return response;

        } catch (Exception e) {
            log.error("Content generation failed for user: {} error: {}",
                    currentUser.getUsername(), e.getMessage(), e);

            // Handle failure case
            return handleGenerationFailure(request, currentUser, e);
        }
    }

    /**
     * Process content generation asynchronously.
     *
     * For long-running tasks, this method allows asynchronous processing
     * while immediately returning a task ID to the user.
     *
     * @param request The content generation request
     * @param currentUser The user making the request
     * @return The task ID for tracking progress
     */
    @Transactional
    public String processContentGenerationAsync(ContentGenerationRequest request, User currentUser) {
        log.info("Starting async content generation for user: {}", currentUser.getUsername());

        // Create the task immediately
        Task task = createTask(request, currentUser);

        // Process asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                // Call the synchronous method internally
                processContentGeneration(request, currentUser);
            } catch (Exception e) {
                log.error("Async content generation failed for task: {}", task.getTaskId(), e);
                handleAsyncFailure(task, e);
            }
        });

        return task.getTaskId();
    }

    /**
     * Get the status of a task.
     *
     * Allows clients to check the progress of content generation,
     * especially useful for asynchronous processing.
     *
     * @param taskId The task identifier
     * @return The current task status and results (if completed)
     */
    public Task getTaskStatus(String taskId) {
        log.debug("Retrieving status for task: {}", taskId);

        return taskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    /**
     * Create a new task entity from the request.
     *
     * Generates a unique task ID and stores the request parameters
     * in the database for tracking and auditing.
     *
     * @param request The content generation request
     * @param user The user creating the task
     * @return The created task entity
     */
    private Task createTask(ContentGenerationRequest request, User user) {
        // Generate unique task ID
        String taskId = "task-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);

        // Convert key messages to JSON string (simplified approach)
        String keyMessagesJson = request.getKeyMessages() != null
                ? String.join(",", request.getKeyMessages())
                : null;

        // Create task entity
        Task task = Task.builder()
                .taskId(taskId)
                .contentType(request.getContentType())
                .brandVoice(request.getBrandVoice())
                .topic(request.getTopic())
                .platform(request.getPlatform())
                .targetAudience(request.getTargetAudience())
                .keyMessages(keyMessagesJson)
                .status(Task.TaskStatus.CREATED)
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return taskRepository.save(task);
    }

    /**
     * Get default brand guidelines.
     *
     * Temporary implementation until BrandGuidelineService is available.
     * Returns a basic set of guidelines for initial testing.
     */
    private BrandGuideline getDefaultBrandGuidelines() {
        // Return a simple default guideline for now
        return BrandGuideline.builder()
                .brandName("Default Brand")
                .voiceGuidelines("Professional yet approachable tone")
                .messagingGuidelines("Focus on value proposition and customer benefits")
                .platformGuidelines("Adapt content length and style to platform requirements")
                .build();
    }

    /**
     * Build the request object for the Python Content Agent.
     *
     * Combines the user request with brand guidelines and context
     * to create a comprehensive request for the Content Agent.
     *
     * @param request The original user request
     * @param brandGuidelines The brand guidelines to include
     * @return The request object for Python service
     */
    private PythonServiceClient.PythonGenerationRequest buildPythonRequest(
            ContentGenerationRequest request, BrandGuideline brandGuidelines) {

        return PythonServiceClient.PythonGenerationRequest.builder()
                .contentType(request.getContentType())
                .brandVoice(request.getBrandVoice())
                .topic(request.getTopic())
                .platform(request.getPlatform())
                .targetAudience(request.getTargetAudience())
                .keyMessages(request.getKeyMessages())
                .brandGuidelines(extractBrandGuidelinesForPython(brandGuidelines))
                .additionalContext(request.getAdditionalContext())
                .lengthPreference(request.getLengthPreference())
                .includeHashtags(request.getIncludeHashtags())
                .callToAction(request.getCallToAction())
                .build();
    }

    /**
     * Extract relevant brand guidelines for the Python service.
     *
     * Converts the brand guidelines entity into a format
     * suitable for sending to the Python Content Agent.
     *
     * @param brandGuidelines The brand guidelines entity
     * @return Simplified brand guidelines string
     */
    private String extractBrandGuidelinesForPython(BrandGuideline brandGuidelines) {
        if (brandGuidelines == null) {
            return null;
        }

        // Combine all guideline fields into a single context string
        StringBuilder guidelines = new StringBuilder();

        if (brandGuidelines.getVoiceGuidelines() != null) {
            guidelines.append("Voice Guidelines: ").append(brandGuidelines.getVoiceGuidelines()).append(" ");
        }

        if (brandGuidelines.getMessagingGuidelines() != null) {
            guidelines.append("Messaging: ").append(brandGuidelines.getMessagingGuidelines()).append(" ");
        }

        if (brandGuidelines.getPlatformGuidelines() != null) {
            guidelines.append("Platform Guidelines: ").append(brandGuidelines.getPlatformGuidelines());
        }

        return guidelines.toString().trim();
    }

    /**
     * Process a successful response from the Python service.
     *
     * Converts the Python service response into a proper
     * ContentGenerationResponse for the frontend.
     *
     * @param task The task being processed
     * @param pythonResponse Response from Python service
     * @return Formatted response for frontend
     */
    private ContentGenerationResponse processSuccessfulResponse(
            Task task, PythonServiceClient.PythonGenerationResponse pythonResponse) {

        return ContentGenerationResponse.builder()
                .taskId(task.getTaskId())
                .content(pythonResponse.getContent())
                .contentType(task.getContentType())
                .platform(task.getPlatform())
                .brandVoice(task.getBrandVoice())
                .targetAudience(task.getTargetAudience())
                .generationTimeSeconds(pythonResponse.getGenerationTimeSeconds())
                .workflowInfo(buildWorkflowInfo(pythonResponse))
                .suggestions(pythonResponse.getSuggestions())
                .estimatedMetrics(pythonResponse.getEstimatedMetrics())
                .generatedAt(LocalDateTime.now())
                .feedbackEnabled(true)
                .build();
    }

    /**
     * Build workflow information from Python response.
     *
     * Extracts workflow details to show users what processing
     * steps were performed during content generation.
     *
     * @param pythonResponse Response from Python service
     * @return Workflow information object
     */
    private ContentGenerationResponse.WorkflowInfo buildWorkflowInfo(
            PythonServiceClient.PythonGenerationResponse pythonResponse) {

        // Extract evaluation data if available
        var evaluation = pythonResponse.getEvaluation();

        // Get workflow info if available, handle both null cases
        PythonServiceClient.WorkflowInfo workflowInfo = pythonResponse.getWorkflowInfo();

        // Extract values with null checks
        Boolean evaluationPerformed = workflowInfo != null ? workflowInfo.getEvaluationPerformed() : false;
        Double evaluationScore = workflowInfo != null ? workflowInfo.getEvaluationScore() : (evaluation != null ? evaluation.getScore() : null);
        Boolean optimizationPerformed = pythonResponse.getOptimizationPerformed() != null ?
                pythonResponse.getOptimizationPerformed() : (workflowInfo != null ? workflowInfo.getOptimizationPerformed() : false);
        Integer optimizationIterations = workflowInfo != null && workflowInfo.getOptimizationIterations() != null ?
                workflowInfo.getOptimizationIterations() : (optimizationPerformed ? 1 : 0);
        String modelUsed = pythonResponse.getModelUsed() != null ?
                pythonResponse.getModelUsed() : (workflowInfo != null ? workflowInfo.getModelUsed() : "Unknown");

        // For debugging
        log.info("Building workflow info from Python response:");
        log.info("evaluationPerformed: {}", evaluationPerformed);
        log.info("evaluationScore: {}", evaluationScore);
        log.info("optimizationPerformed: {}", optimizationPerformed);
        log.info("optimizationIterations: {}", optimizationIterations);
        log.info("modelUsed: {}", modelUsed);

        return ContentGenerationResponse.WorkflowInfo.builder()
                .initialGenerationCompleted(true)
                .evaluationPerformed(evaluationPerformed) // Default to true
                .evaluationScore(evaluationScore)
                .optimizationPerformed(optimizationPerformed)
                .optimizationIterations(optimizationIterations)
                .modelUsed(modelUsed)
                .build();
    }

    /**
     * Update the task entity with successful results.
     *
     * Marks the task as completed and stores the generated content.
     *
     * @param task The task to update
     * @param pythonResponse The successful response
     */
    private void updateTaskWithSuccess(Task task, PythonServiceClient.PythonGenerationResponse pythonResponse) {
        task.setStatus(Task.TaskStatus.COMPLETED);
        task.setGeneratedContent(pythonResponse.getContent());
        task.setUpdatedAt(LocalDateTime.now());

        // Store metadata as JSON string (simplified)
        task.setMetadata(String.format("{\"model\": \"%s\", \"generationTime\": %.2f}",
                pythonResponse.getModelUsed(), pythonResponse.getGenerationTimeSeconds()));

        taskRepository.save(task);
    }

    /**
     * Handle generation failure cases.
     *
     * When content generation fails, this method ensures
     * proper error handling and user feedback.
     *
     * @param request The original request
     * @param user The user who made the request
     * @param exception The exception that occurred
     * @return Error response for the frontend
     */
    private ContentGenerationResponse handleGenerationFailure(
            ContentGenerationRequest request, User user, Exception exception) {

        log.error("Handling generation failure for user: {}", user.getUsername(), exception);

        try {
            // Create a failure task for auditing
            Task failedTask = createTask(request, user);
            failedTask.setStatus(Task.TaskStatus.FAILED);
            failedTask.setErrorMessage(exception.getMessage());
            failedTask.setUpdatedAt(LocalDateTime.now());
            taskRepository.save(failedTask);

            return ContentGenerationResponse.builder()
                    .taskId(failedTask.getTaskId())
                    .contentType(request.getContentType())
                    .generatedAt(LocalDateTime.now())
                    .workflowInfo(ContentGenerationResponse.WorkflowInfo.builder()
                            .initialGenerationCompleted(false)
                            .evaluationPerformed(false)
                            .optimizationPerformed(false)
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("Failed to save error task", e);
            throw new RuntimeException("Content generation failed: " + exception.getMessage(), exception);
        }
    }

    /**
     * Handle asynchronous task failure.
     *
     * Updates the task status when async processing fails.
     *
     * @param task The task that failed
     * @param exception The exception that occurred
     */
    private void handleAsyncFailure(Task task, Exception exception) {
        task.setStatus(Task.TaskStatus.FAILED);
        task.setErrorMessage(exception.getMessage());
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
    }

    /**
     * Get tasks for a specific user with pagination.
     *
     * Retrieves the user's content generation history with pagination support.
     *
     * @param user The user whose tasks to retrieve
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @return List of user's tasks
     */
    public List<Task> getUserTasks(User user, int page, int size) {
        log.debug("Retrieving tasks for user: {} page: {} size: {}", user.getUsername(), page, size);

        List<Task> allUserTasks = taskRepository.findByCreatedByOrderByCreatedAtDesc(user);

        // Calculate start and end indices for pagination
        int start = page * size;
        int end = Math.min(start + size, allUserTasks.size());

        // Return the requested page
        if (start >= allUserTasks.size()) {
            return List.of(); // Empty list if page is beyond available data
        }

        return allUserTasks.subList(start, end);
    }

    /**
     * Cancel a content generation task.
     *
     * Cancels a task if it's in a cancellable state and the user has permission.
     * Only tasks in CREATED or PROCESSING status can be cancelled.
     *
     * @param taskId The task identifier
     * @param user The user requesting cancellation
     * @throws IllegalArgumentException if task is not found
     * @throws SecurityException if user doesn't have permission
     * @throws IllegalStateException if task cannot be cancelled
     */
    @Transactional
    public void cancelTask(String taskId, User user) {
        log.info("Cancelling task: {} requested by user: {}", taskId, user.getUsername());

        // Find the task
        Task task = taskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // Check authorization (user can cancel their own tasks, admins can cancel any task)
        if (!task.getCreatedBy().getId().equals(user.getId()) && !user.getRole().equals(User.Role.ADMIN)) {
            log.warn("User {} attempted to cancel task {} belonging to user {}",
                    user.getUsername(), taskId, task.getCreatedBy().getUsername());
            throw new SecurityException("Access denied - cannot cancel task belonging to another user");
        }

        // Check if task can be cancelled
        if (task.getStatus() == Task.TaskStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed task");
        }

        if (task.getStatus() == Task.TaskStatus.FAILED) {
            throw new IllegalStateException("Cannot cancel failed task");
        }

        // Update task status to cancelled
        task.setStatus(Task.TaskStatus.FAILED); // Using FAILED for now, should add CANCELLED status
        task.setErrorMessage("Task cancelled by user");
        task.setUpdatedAt(LocalDateTime.now());

        taskRepository.save(task);

        log.info("Task cancelled successfully: {} by user: {}", taskId, user.getUsername());
    }

    /**
     * Get recent tasks for a user (convenience method).
     *
     * Returns the most recent tasks for a user, useful for dashboard displays.
     *
     * @param user The user whose recent tasks to retrieve
     * @param limit Maximum number of tasks to return
     * @return List of recent tasks
     */
    public List<Task> getRecentUserTasks(User user, int limit) {
        log.debug("Retrieving {} recent tasks for user: {}", limit, user.getUsername());

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Task> recentTasks = taskRepository.findByCreatedByAndCreatedAtAfterOrderByCreatedAtDesc(user, thirtyDaysAgo);

        // Limit the results
        return recentTasks.stream()
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional // Ensures the operation is atomic
    public void deleteTask(String taskId) {
        // Find the task by its unique taskId (external ID)
        Task task = taskRepository.findByTaskId(taskId)
                .orElseThrow(() -> {
                    log.warn("Attempted to delete non-existent task with ID: {}", taskId);
                    return new IllegalArgumentException("Task not found with ID: " + taskId);
                });

        // Delete the found task
        taskRepository.delete(task);
        log.info("Task with ID {} successfully deleted.", taskId);
    }

    /**
     * Retrieves all tasks from the database and converts them to TaskDto.
     *
     * @return A list of all tasks as TaskDto objects.
     */
    @Transactional(readOnly = true) // Read-only transaction for fetching data
    public List<TaskDto> getAllTasks() {
        List<Task> tasks = taskRepository.findAll();
        return TaskMapper.toDtoList(tasks); // Use the mapper to convert to DTOs
    }

    /**
     * Retrieves a single task by its unique external task ID and converts it to TaskDto.
     *
     * @param taskId The unique task identifier string (external ID).
     * @return The TaskDto if found.
     * @throws IllegalArgumentException if the task is not found.
     */
    @Transactional(readOnly = true) // Read-only transaction for fetching data
    public TaskDto getTaskByExternalId(String taskId) {
        Task task = taskRepository.findByTaskId(taskId)
                .orElseThrow(() -> {
                    log.warn("Attempted to retrieve non-existent task with ID: {}", taskId);
                    return new IllegalArgumentException("Task not found with ID: " + taskId);
                });
        return TaskMapper.toDto(task); // Use the mapper to convert to DTO
    }

    /**
     * Generate content ONLY - don't save to database
     * Returns the generated content for user preview
     */
    public ContentGenerationResponse generateContentOnly(ContentGenerationRequest request, User currentUser) {
        log.info("Generating content only for user: {} content type: {}", 
                currentUser.getUsername(), request.getContentType());

        try {
            // Load brand guidelines
            BrandGuideline brandGuidelines = getDefaultBrandGuidelines();
            log.debug("Using default brand guidelines");

            // Prepare request for Python service
            var pythonRequest = buildPythonRequest(request, brandGuidelines);

            // Call Python Content Agent
            log.info("Calling Python Content Agent for content generation");
            var pythonResponse = pythonServiceClient.generateContent(pythonRequest);

            // Create response WITHOUT saving to database
            ContentGenerationResponse response = ContentGenerationResponse.builder()
                    .taskId(null) // No task ID since we haven't saved yet
                    .content(pythonResponse.getContent())
                    .contentType(request.getContentType())
                    .platform(request.getPlatform())
                    .brandVoice(request.getBrandVoice())
                    .targetAudience(request.getTargetAudience())
                    .generationTimeSeconds(pythonResponse.getGenerationTimeSeconds())
                    .workflowInfo(buildWorkflowInfo(pythonResponse))
                    .suggestions(pythonResponse.getSuggestions())
                    .estimatedMetrics(pythonResponse.getEstimatedMetrics())
                    .generatedAt(LocalDateTime.now())
                    .feedbackEnabled(true)
                    .build();

            log.info("Content generation completed successfully");
            return response;

        } catch (Exception e) {
            log.error("Content generation failed for user: {} error: {}", 
                    currentUser.getUsername(), e.getMessage(), e);
            throw new RuntimeException("Content generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Save the generated content to database
     * Called when user decides to save the content
     */
    @Transactional
    public String saveGeneratedContent(SaveContentRequest saveRequest, User currentUser) {
        log.info("Saving generated content for user: {}", currentUser.getUsername());

        try {
            // Create task with the generated content
            Task task = createTaskFromSaveRequest(saveRequest, currentUser);
            
            // Mark as completed since content is already generated
            task.setStatus(Task.TaskStatus.COMPLETED);
            task.setGeneratedContent(saveRequest.getContent());
            task.setUpdatedAt(LocalDateTime.now());

            // Store metadata
            task.setMetadata(String.format("{\"model\": \"%s\", \"generationTime\": %.2f}",
                    saveRequest.getModelUsed(), saveRequest.getGenerationTimeSeconds()));

            task = taskRepository.save(task);

            log.info("Content saved successfully with task ID: {}", task.getTaskId());
            return task.getTaskId();

        } catch (Exception e) {
            log.error("Failed to save content for user: {}", currentUser.getUsername(), e);
            throw new RuntimeException("Failed to save content: " + e.getMessage(), e);
        }
    }

    /**
     * Create a task from save request
     */
    private Task createTaskFromSaveRequest(SaveContentRequest saveRequest, User user) {
        String taskId = "task-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);

        String keyMessagesJson = saveRequest.getKeyMessages() != null
                ? String.join(",", saveRequest.getKeyMessages())
                : null;

        return Task.builder()
                .taskId(taskId)
                .contentType(saveRequest.getContentType())
                .brandVoice(saveRequest.getBrandVoice())
                .topic(saveRequest.getTopic())
                .platform(saveRequest.getPlatform())
                .targetAudience(saveRequest.getTargetAudience())
                .keyMessages(keyMessagesJson)
                .status(Task.TaskStatus.COMPLETED) // Already completed
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}