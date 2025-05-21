package com.exjobb.backend.dto;

import lombok.Data;
import java.util.List;

/**
 * Request DTO for content generation.
 * Matches the format expected by the frontend and sent to Python service.
 */
@Data
public class ContentGenerationRequest {

    private String contentType;
    private String brandVoice;
    private String topic;
    private String platform;
    private String targetAudience;
    private List<String> keyMessages;
    private String brandGuidelines;
    private String additionalContext;
    private String lengthPreference;
    private Boolean includeHashtags;
    private String callToAction;
}