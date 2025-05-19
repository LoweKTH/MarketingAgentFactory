package com.exjobb.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * DTO for content generation requests from the frontend.
 *
 * This class defines the structure and validation rules for requests
 * to generate marketing content. It's used by the API Gateway to
 * receive and validate user input before passing it to the Task Router.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentGenerationRequest {

    /**
     * Type of content to generate (e.g., "social_post", "blog_post", "ad_copy").
     * This is required and determines how the Content Agent processes the request.
     */
    @NotBlank(message = "Content type is required")
    @Size(max = 50, message = "Content type must not exceed 50 characters")
    private String contentType;

    /**
     * Brand voice to use for the content (e.g., "professional", "casual", "technical").
     * This is required to ensure content matches the brand's tone.
     */
    @NotBlank(message = "Brand voice is required")
    @Size(max = 50, message = "Brand voice must not exceed 50 characters")
    private String brandVoice;

    /**
     * Main topic or subject for the content.
     * This is the core theme around which content will be generated.
     */
    @NotBlank(message = "Topic is required")
    @Size(max = 500, message = "Topic must not exceed 500 characters")
    private String topic;

    /**
     * Target platform for the content (e.g., "linkedin", "twitter", "facebook").
     * Optional - if not specified, generic content will be generated.
     */
    @Size(max = 50, message = "Platform must not exceed 50 characters")
    private String platform;

    /**
     * Description of the target audience.
     * Optional - helps tailor content to the appropriate demographic.
     */
    @Size(max = 200, message = "Target audience must not exceed 200 characters")
    private String targetAudience;

    /**
     * List of key messages that must be included in the content.
     * Optional - these are important points that should be covered.
     */
    @Size(max = 10, message = "Maximum 10 key messages allowed")
    private List<@Size(max = 100, message = "Each key message must not exceed 100 characters") String> keyMessages;

    /**
     * Desired length or style indicator (e.g., "short", "medium", "long").
     * Optional - provides guidance on content length and complexity.
     */
    @Size(max = 20, message = "Length preference must not exceed 20 characters")
    private String lengthPreference;

    /**
     * Whether to include hashtags in the generated content.
     * Optional - mainly relevant for social media content.
     */
    private Boolean includeHashtags;

    /**
     * Specific call-to-action to include.
     * Optional - if specified, this CTA will be incorporated into the content.
     */
    @Size(max = 200, message = "Call to action must not exceed 200 characters")
    private String callToAction;

    /**
     * Additional context or special instructions for content generation.
     * Optional - provides any extra guidance not covered by other fields.
     */
    @Size(max = 500, message = "Additional context must not exceed 500 characters")
    private String additionalContext;
}