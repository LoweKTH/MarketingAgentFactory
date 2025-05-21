package com.exjobb.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for content generation.
 * Enhanced to support smart threshold evaluator-optimizer workflow results.
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
    private EvaluationDetails evaluationDetails;
    private List<String> suggestions;
    private Map<String, Object> estimatedMetrics;
    private LocalDateTime generatedAt;
    private Boolean feedbackEnabled;
    private OptimizationDetails optimizationDetails;

    /**
     * Information about the workflow steps performed.
     */
    @Data
    @Builder
    public static class WorkflowInfo {
        private Boolean initialGenerationCompleted;
        @JsonProperty("evaluationPerformed")
        private Boolean evaluationPerformed;
        private Double evaluationScore;
        private Boolean optimizationPerformed;
        private String optimizationType;
        private Integer optimizationIterations;
        private String modelUsed;
    }

    /**
     * Details about the content evaluation.
     */
    @Data
    @Builder
    public static class EvaluationDetails {
        private Map<String, Double> criteriaScores;
        private List<String> strengths;
        private List<String> improvements;
    }

    /**
     * Details about optimization when performed.
     */
    @Data
    @Builder
    public static class OptimizationDetails {
        private String initialContent;
        private String optimizedContent;
        private String optimizationType;
        private Double optimizationTime;
        private Double initialGenerationTime;
        private String improvementReason;
        private EvaluationComparison evaluationComparison;
    }

    /**
     * Comparison between initial and optimized evaluations.
     */
    @Data
    @Builder
    public static class EvaluationComparison {
        private Double initialScore;
        private Double optimizedScore;
        private Double scoreDifference;
        private Map<String, Object> initialEvaluation;
        private Map<String, Object> optimizedEvaluation;
    }
}