package com.exjobb.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing brand guidelines and identity information.
 *
 * This entity stores brand-related configuration that guides
 * content generation to ensure consistency with brand identity.
 * The Task Router uses this information when calling the Python
 * Content Agent.
 */
@Entity
@Table(name = "brand_guidelines")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class BrandGuideline {

    /**
     * Unique identifier for the brand guideline entry.
     * Generated automatically by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name/identifier for this brand guideline set.
     * Allows multiple brand configurations (e.g., different companies).
     */
    @Column(unique = true, nullable = false)
    private String brandName;

    /**
     * Brand voice definitions stored as JSON.
     * Contains tone descriptions, examples, and guidelines for different voices
     * (e.g., professional, casual, technical).
     */
    @Column(length = 3000)
    private String voiceGuidelines;

    /**
     * Visual identity information stored as JSON.
     * Includes colors, typography, logo usage guidelines, etc.
     * While the MVP focuses on text, this prepares for future image generation.
     */
    @Column(length = 2000)
    private String visualIdentity;

    /**
     * Key messaging and prohibited terms stored as JSON.
     * Contains must-include messages and words/phrases to avoid.
     */
    @Column(length = 2000)
    private String messagingGuidelines;

    /**
     * Target audience definitions stored as JSON.
     * Describes different audience segments and how to communicate with them.
     */
    @Column(length = 2000)
    private String targetAudiences;

    /**
     * Platform-specific guidelines stored as JSON.
     * Contains optimization rules for different social media platforms,
     * character limits, hashtag preferences, etc.
     */
    @Column(length = 2000)
    private String platformGuidelines;

    /**
     * Whether this brand guideline set is currently active.
     * Only active guidelines are used for content generation.
     */
    @Builder.Default
    private Boolean active = true;

    /**
     * Version number for tracking guideline updates.
     * Helps maintain consistency when guidelines change.
     */
    @Builder.Default
    private Integer version = 1;

    /**
     * Timestamp when the brand guidelines were created.
     * Automatically set by JPA auditing.
     */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the brand guidelines were last updated.
     * Automatically updated by JPA auditing.
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;
}