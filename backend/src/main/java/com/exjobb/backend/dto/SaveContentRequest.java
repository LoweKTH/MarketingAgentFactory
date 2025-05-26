package com.exjobb.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveContentRequest {
    private String content;
    private String contentType;
    private String brandVoice;
    private String topic;
    private String platform;
    private String targetAudience;
    private List<String> keyMessages;
    private String additionalContext;
    private String lengthPreference;
    private Boolean includeHashtags;
    private String callToAction;
    private Double generationTimeSeconds;
    private String modelUsed;
}
