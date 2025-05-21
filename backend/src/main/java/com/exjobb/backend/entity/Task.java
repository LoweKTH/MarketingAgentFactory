package com.exjobb.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing a content generation task.
 *
 * Tasks store information about content generation requests,
 * their status, and the generated content.
 */
@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Task {

    /**
     * Internal database ID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * External task ID visible to users.
     * Used in APIs and for tracking.
     */
    @Column(nullable = false, unique = true)
    private String taskId;

    /**
     * Type of content being generated.
     * Examples: "social_post", "blog_post", "ad_copy"
     */
    @Column(nullable = false)
    private String contentType;

    /**
     * Brand voice for the content.
     * Examples: "professional", "casual", "friendly"
     */
    @Column(nullable = false)
    private String brandVoice;

    /**
     * Main topic of the content.
     */
    @Column(nullable = false, length = 1000)
    private String topic;

    /**
     * Target platform for the content.
     * Examples: "linkedin", "facebook", "twitter"
     */
    private String platform;

    /**
     * Target audience for the content.
     */
    private String targetAudience;

    /**
     * Key messages to include in the content.
     * Stored as a comma-separated string.
     */
    @Column(length = 2000)
    private String keyMessages;

    /**
     * Current status of the task.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    /**
     * Error message if the task failed.
     */
    @Column(length = 1000)
    private String errorMessage;

    /**
     * The generated content.
     */
    @Column(length = 5000)
    private String generatedContent;

    /**
     * Additional metadata about the task.
     * Stored as a JSON string.
     */
    @Column(length = 2000)
    private String metadata;

    /**
     * User who created the task.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User createdBy;

    /**
     * Timestamp when the task was created.
     */
    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * Timestamp when the task was last updated.
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Possible statuses for a task.
     */
    public enum TaskStatus {
        CREATED,     // Just created, not yet processed
        PROCESSING,  // Currently being processed
        COMPLETED,   // Successfully completed
        FAILED,      // Failed to complete
        CANCELLED    // Cancelled by user
    }
}