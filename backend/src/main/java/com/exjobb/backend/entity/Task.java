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
 * Entity representing a content generation task.
 *
 * This entity stores information about each content generation request,
 * including the request parameters, current status, and results.
 * It serves as the main tracking mechanism for the task workflow.
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
     * Unique identifier for the task.
     * Generated automatically by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable task identifier for easier tracking.
     * Format: "task-{timestamp}-{random}"
     */
    @Column(unique = true, nullable = false)
    private String taskId;

    /**
     * Type of content being generated (e.g., "social_post", "blog_post").
     * Used by the Task Router to determine processing approach.
     */
    @Column(nullable = false)
    private String contentType;

    /**
     * Brand voice to be used (e.g., "professional", "casual").
     * Passed to the Python Content Agent for tone consistency.
     */
    @Column(nullable = false)
    private String brandVoice;

    /**
     * Main topic/subject for the content.
     * The core theme around which content will be generated.
     */
    @Column(nullable = false, length = 1000)
    private String topic;

    /**
     * Target platform for the content (e.g., "linkedin", "twitter").
     * Used for platform-specific optimization.
     */
    private String platform;

    /**
     * Target audience description.
     * Helps tailor the content to the appropriate demographic.
     */
    private String targetAudience;

    /**
     * Key messages to include in the content (stored as JSON string).
     * Important points that must be covered in the generated content.
     */
    @Column(length = 2000)
    private String keyMessages;

    /**
     * Current status of the task.
     * Values: CREATED, PROCESSING, COMPLETED, FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    /**
     * The generated content result (if completed successfully).
     * Stores the final output from the Content Agent.
     */
    @Column(length = 5000)
    private String generatedContent;

    /**
     * Metadata about the generation process (stored as JSON).
     * Includes information like generation time, model used, etc.
     */
    @Column(length = 2000)
    private String metadata;

    /**
     * Error message if the task failed.
     * Helps with debugging and user feedback.
     */
    @Column(length = 1000)
    private String errorMessage;

    /**
     * User who created this task.
     * Links the task to the requesting user.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User createdBy;

    /**
     * Timestamp when the task was created.
     * Automatically set by JPA auditing.
     */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the task was last updated.
     * Automatically updated by JPA auditing.
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;


    /**
     * Enum defining possible task statuses.
     * Helps track the task through its lifecycle.
     */
    public enum TaskStatus {
        CREATED,     // Task created but not yet sent to Python service
        PROCESSING,  // Currently being processed by Content Agent
        COMPLETED,   // Successfully completed with results
        FAILED,      // Failed with error message
        CANCELLED    // Cancelled by user or system
    }
}