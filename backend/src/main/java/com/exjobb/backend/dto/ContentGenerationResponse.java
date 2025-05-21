package com.exjobb.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for content generation.
 * Contains the generated content and metadata from the evaluator-optimizer workflow.
 */
@Data
@Builder
public class ContentGenerationResponse {

    private String taskId;
    private String content;
    private String contentType;
    private String platform;
    private String brandVoice;
    private String targetAudience;
    private Double generationTimeSeconds;
    private WorkflowInfo workflowInfo;
    private List<String> suggestions;
    private Map<String, Object> estimatedMetrics;
    private LocalDateTime generatedAt;
    private Boolean feedbackEnabled;

    /**
     * Information about the workflow steps performed.
     */
    @Data
    @Builder
    public static class WorkflowInfo {
        private Boolean initialGenerationCompleted;
        private Boolean evaluationPerformed;
        private Double evaluationScore;
        private Boolean optimizationPerformed;
        private Integer optimizationIterations;
        private String modelUsed;
    }
}