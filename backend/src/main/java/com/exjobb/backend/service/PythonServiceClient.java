package com.exjobb.backend.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Client service for communicating with the Python Content Agent.
 *
 * This service handles all HTTP communication with the Python service,
 * including request formatting, error handling, and response parsing.
 * It acts as the bridge between the Spring Boot backend and Python AI service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PythonServiceClient {

    private final WebClient webClient;

    @Value("${python.service.url:http://localhost:5000}")
    private String pythonServiceUrl;

    @Value("${python.service.timeout:30000}")
    private long timeoutMs;

    /**
     * Generate content using the Python Content Agent.
     *
     * Sends the content generation request to the Python service
     * and handles the response, including error cases.
     *
     * @param request The content generation request
     * @return The response from the Python service
     * @throws RuntimeException if the request fails
     */
    public PythonGenerationResponse generateContent(PythonGenerationRequest request) {
        log.info("Sending content generation request to Python service: {}", request.getContentType());
        log.debug("Request details: topic='{}', platform='{}'", request.getTopic(), request.getPlatform());

        try {
            // Send POST request to Python service
            ResponseEntity<PythonGenerationResponse> response = webClient
                    .post()
                    .uri(pythonServiceUrl + "/generate")
                    .bodyValue(request)
                    .retrieve()
                    .toEntity(PythonGenerationResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

            // Add logging to debug the response
            assert response != null;
            log.info("Raw Python service response: {}", response.getBody());
            if (response.getBody() != null && response.getBody().getWorkflowInfo() != null) {
                log.info("evaluationPerformed: {}", response.getBody().getWorkflowInfo().getEvaluationPerformed());
            } else {
                log.warn("Response body or workflowInfo is null");
            }

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Successfully received response from Python service");
                log.debug("Response content length: {} characters",
                        response.getBody().getContent() != null ? response.getBody().getContent().length() : 0);
                return response.getBody();
            } else {
                log.error("Invalid response from Python service: status={}, body={}",
                        response != null ? response.getStatusCode() : "null",
                        response != null ? response.getBody() : "null");
                throw new RuntimeException("Invalid response from Python service");
            }

        } catch (WebClientResponseException e) {
            log.error("HTTP error from Python service: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Python service returned error: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Failed to communicate with Python service", e);
            throw new RuntimeException("Failed to communicate with Python service: " + e.getMessage(), e);
        }
    }

    /**
     * Check the health of the Python service.
     *
     * Performs a simple health check to ensure the Python service
     * is running and accessible.
     *
     * @return True if the service is healthy, false otherwise
     */
    public boolean checkPythonServiceHealth() {
        log.debug("Checking Python service health");

        try {
            ResponseEntity<Map> response = webClient
                    .get()
                    .uri(pythonServiceUrl + "/health")
                    .retrieve()
                    .toEntity(Map.class)
                    .timeout(Duration.ofMillis(5000)) // Shorter timeout for health checks
                    .block();

            boolean isHealthy = response != null
                    && response.getStatusCode() == HttpStatus.OK
                    && response.getBody() != null
                    && "healthy".equals(response.getBody().get("status"));

            log.debug("Python service health check result: {}", isHealthy);
            return isHealthy;

        } catch (Exception e) {
            log.warn("Python service health check failed", e);
            return false;
        }
    }

    /**
     * Get streaming updates for a task.
     *
     * Connects to the Python service's streaming endpoint to receive
     * real-time updates about content generation progress.
     *
     * @param taskId The task ID to stream updates for
     * @return Mono stream of task updates
     */
    public Mono<String> streamTaskUpdates(String taskId) {
        log.debug("Starting stream for task: {}", taskId);

        return webClient
                .get()
                .uri(pythonServiceUrl + "/stream/" + taskId)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .doOnSuccess(response -> log.debug("Received stream update for task: {}", taskId))
                .doOnError(error -> log.error("Stream error for task: {}", taskId, error));
    }

    // ================================================
    // Nested classes for request/response structures
    // ================================================

    /**
     * Request structure for Python Content Agent.
     *
     * This class defines the structure of requests sent to the
     * Python service for content generation.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PythonGenerationRequest {

        /**
         * Type of content to generate (e.g., "social_post", "blog_post").
         */
        private String contentType;

        /**
         * Brand voice to use (e.g., "professional", "casual").
         */
        private String brandVoice;

        /**
         * Main topic for the content.
         */
        private String topic;

        /**
         * Target platform (e.g., "linkedin", "twitter").
         */
        private String platform;

        /**
         * Target audience description.
         */
        private String targetAudience;

        /**
         * Key messages to include.
         */
        private List<String> keyMessages;

        /**
         * Brand guidelines context for the Python service.
         */
        private String brandGuidelines;

        /**
         * Additional context or special instructions.
         */
        private String additionalContext;

        /**
         * Length preference (e.g., "short", "medium", "long").
         */
        private String lengthPreference;

        /**
         * Whether to include hashtags.
         */
        private Boolean includeHashtags;

        /**
         * Specific call-to-action to include.
         */
        private String callToAction;
    }

    /**
     * Response structure from Python Content Agent.
     *
     * This class defines the structure of responses received from the
     * Python service after content generation.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PythonGenerationResponse {

        /**
         * The generated content.
         */
        private String content;

        /**
         * Type of content that was generated.
         */
        private String contentType;

        /**
         * Platform the content was optimized for.
         */
        private String platform;

        /**
         * Brand voice used.
         */
        private String brandVoice;

        /**
         * Target audience.
         */
        private String targetAudience;

        /**
         * Time taken for generation (in seconds).
         */
        private Double generationTimeSeconds;

        /**
         * Information about workflow steps performed.
         */
        private WorkflowInfo workflowInfo;

        /**
         * Evaluation results from the evaluator-optimizer workflow.
         */
        private EvaluationResult evaluation;

        /**
         * Metadata about the generation process.
         */
        private Map<String, Object> metadata;

        /**
         * Whether optimization was performed.
         */
        private Boolean optimizationPerformed;

        /**
         * Suggestions for improving the content.
         */
        private List<String> suggestions;

        /**
         * Estimated performance metrics.
         */
        private Map<String, Object> estimatedMetrics;

        /**
         * Name/version of the model used.
         */
        private String modelUsed;
    }

    /**
     * Workflow information from the Python service.
     *
     * Contains details about the workflow steps performed during content generation.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkflowInfo {
        private Boolean initialGenerationCompleted;
        private Boolean evaluationPerformed;
        private Double evaluationScore;
        private Boolean optimizationPerformed;
        private String optimizationType;
        private Integer optimizationIterations;
        private String modelUsed;
    }

    /**
     * Evaluation results from the Python service.
     *
     * Contains detailed information about how the evaluator
     * component assessed the generated content.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvaluationResult {

        /**
         * Overall quality score (1-10).
         */
        private Double score;

        /**
         * Strengths identified by the evaluator.
         */
        private List<String> strengths;

        /**
         * Areas for improvement identified.
         */
        private List<String> improvements;

        /**
         * Whether the evaluator recommended optimization.
         */
        private Boolean needsImprovement;

        /**
         * Raw evaluation text from the LLM evaluator.
         */
        private String rawEvaluation;
    }
}