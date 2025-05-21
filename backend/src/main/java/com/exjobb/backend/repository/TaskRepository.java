package com.exjobb.backend.repository;

import com.exjobb.backend.entity.Task;
import com.exjobb.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Task entity operations.
 *
 * Provides data access methods for managing content generation tasks.
 * Extends JpaRepository to get standard CRUD operations plus custom queries
 * for task management and analytics.
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Find a task by its unique task ID.
     * The task ID is the human-readable identifier (not the database ID).
     *
     * @param taskId The unique task identifier string
     * @return Optional containing the task if found
     */
    Optional<Task> findByTaskId(String taskId);

    /**
     * Find all tasks created by a specific user.
     * Ordered by creation date (newest first).
     *
     * @param user The user who created the tasks
     * @return List of tasks created by the user
     */
    List<Task> findByCreatedByOrderByCreatedAtDesc(User user);

    /**
     * Find tasks created by a specific user after a given date.
     * Ordered by creation date (newest first).
     * Used for retrieving recent user activity and implementing pagination.
     *
     * @param user The user who created the tasks
     * @param createdAt The date threshold (tasks created after this date)
     * @return List of tasks created by the user after the specified date
     */
    List<Task> findByCreatedByAndCreatedAtAfterOrderByCreatedAtDesc(User user, LocalDateTime createdAt);

    /**
     * Find tasks by status.
     * Useful for monitoring and administration.
     *
     * @param status The task status to filter by
     * @return List of tasks with the specified status
     */
    List<Task> findByStatus(Task.TaskStatus status);

    /**
     * Find tasks by content type for analytics.
     * Helps understand which content types are most popular.
     *
     * @param contentType The type of content (e.g., "social_post")
     * @return List of tasks of the specified content type
     */
    List<Task> findByContentType(String contentType);

    /**
     * Find recent tasks for a user (last 30 days).
     * Useful for user dashboards and recent activity.
     *
     * @param user The user whose tasks to retrieve
     * @param since The date from which to retrieve tasks
     * @return List of recent tasks by the user
     */
    @Query("SELECT t FROM Task t WHERE t.createdBy = :user AND t.createdAt >= :since ORDER BY t.createdAt DESC")
    List<Task> findRecentTasksByUser(@Param("user") User user, @Param("since") LocalDateTime since);

    /**
     * Count tasks by status for dashboard statistics.
     * Provides quick overview of system activity.
     *
     * @param status The status to count
     * @return Number of tasks with the specified status
     */
    long countByStatus(Task.TaskStatus status);

    /**
     * Find tasks created within a date range.
     * Useful for reporting and analytics.
     *
     * @param start Start date (inclusive)
     * @param end End date (inclusive)
     * @return List of tasks created within the date range
     */
    @Query("SELECT t FROM Task t WHERE t.createdAt BETWEEN :start AND :end ORDER BY t.createdAt DESC")
    List<Task> findTasksInDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Find tasks that are stuck in processing state.
     * Helps identify tasks that may have failed without proper error handling.
     *
     * @param processingTimeout Tasks older than this time that are still processing
     * @return List of potentially stuck tasks
     */
    @Query("SELECT t FROM Task t WHERE t.status = 'PROCESSING' AND t.updatedAt < :processingTimeout")
    List<Task> findStuckTasks(@Param("processingTimeout") LocalDateTime processingTimeout);

    /**
     * Get task completion statistics for a user.
     * Returns count of completed vs total tasks.
     *
     * @param user The user to get statistics for
     * @return Array containing [completed_count, total_count]
     */
    @Query("SELECT " +
            "SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END), " +
            "COUNT(t) " +
            "FROM Task t WHERE t.createdBy = :user")
    Object[] getUserTaskStatistics(@Param("user") User user);

    /**
     * Find tasks by user and status for filtered queries.
     * Useful for showing user's tasks in specific states.
     *
     * @param user The user who created the tasks
     * @param status The task status to filter by
     * @return List of tasks matching the user and status criteria
     */
    List<Task> findByCreatedByAndStatus(User user, Task.TaskStatus status);

    /**
     * Count total tasks for a specific user.
     * Used for pagination calculations and user statistics.
     *
     * @param user The user whose tasks to count
     * @return Total number of tasks created by the user
     */
    long countByCreatedBy(User user);

    /**
     * Find tasks by platform for analytics.
     * Helps understand which platforms are most commonly targeted.
     *
     * @param platform The target platform (e.g., "linkedin", "twitter")
     * @return List of tasks targeting the specified platform
     */
    List<Task> findByPlatform(String platform);

    /**
     * Find failed tasks that can be retried.
     * Used for implementing retry functionality.
     *
     * @param user The user whose failed tasks to retrieve
     * @return List of failed tasks that can potentially be retried
     */
    @Query("SELECT t FROM Task t WHERE t.createdBy = :user AND t.status = 'FAILED' ORDER BY t.updatedAt DESC")
    List<Task> findRetryableTasks(@Param("user") User user);
}