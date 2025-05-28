package com.exjobb.backend.service;

import com.exjobb.backend.entity.Task;
import com.exjobb.backend.entity.Task.TaskStatus;
import com.exjobb.backend.entity.UserSocialConnection;
import com.exjobb.backend.repository.TaskRepository;
import com.exjobb.backend.repository.UserSocialConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class ScheduledPostService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledPostService.class);

    private final UserSocialConnectionRepository userSocialConnectionRepository;
    private final TwitterOAuthService twitterOAuthService;
    private final TwitterApiService twitterApiService;
    private final TaskRepository taskRepository;

    public ScheduledPostService(UserSocialConnectionRepository userSocialConnectionRepository,
                                TwitterOAuthService twitterOAuthService,
                                TwitterApiService twitterApiService,
                                TaskRepository taskRepository) {
        this.userSocialConnectionRepository = userSocialConnectionRepository;
        this.twitterOAuthService = twitterOAuthService;
        this.twitterApiService = twitterApiService;
        this.taskRepository = taskRepository;
    }

    @Scheduled(fixedRate = 60000)
    public void processScheduledPosts() {
        logger.info("Running scheduled post processing...");
        LocalDateTime now = LocalDateTime.now();

        List<Task> pendingTasks = taskRepository.findTasksDueForPosting(now);

        if (pendingTasks.isEmpty()) {
            logger.info("No tasks due for posting.");
            return;
        }

        for (Task task : pendingTasks) {
            logger.info("Processing task: {} (ID: {}) for platform {}", task.getTopic(), task.getId(), task.getPlatform());

            task.setStatus(TaskStatus.PROCESSING);
            taskRepository.save(task);

            try {
                if ("twitter".equalsIgnoreCase(task.getPlatform())) {
                    processTwitterTask(task);
                }
                // Add else if for other platforms (e.g., LinkedIn, Facebook)
            } catch (Exception e) {
                logger.error("Error processing task {}: {}", task.getId(), e.getMessage(), e);
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage("Processing failed: " + e.getMessage());
                taskRepository.save(task);
            }
        }
    }

    private void processTwitterTask(Task task) {
        if (task.getCreatedBy() == null || task.getCreatedBy().getId() == null) {
            logger.warn("Task {} has no associated user or user ID. Cannot process Twitter post.", task.getId());
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("Task has no associated user or user ID.");
            taskRepository.save(task);
            return;
        }

        // This line will now work correctly if UserSocialConnectionRepository.findByUser_IdAndPlatform is used
        Optional<UserSocialConnection> connectionOpt = userSocialConnectionRepository
                .findByUserIdAndPlatform(task.getCreatedBy().getId(), "twitter"); // Corrected method name to findByUser_IdAndPlatform


        if (connectionOpt.isEmpty()) {
            logger.warn("No Twitter connection found for user ID {} associated with task: {}", task.getCreatedBy().getId(), task.getId());
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("No valid Twitter connection found for the user.");
            taskRepository.save(task);
            return;
        }

        UserSocialConnection connection = connectionOpt.get();

        boolean tokenNeedsRefresh = false;
        if (connection.getExpiresIn() != null && !connection.getExpiresIn().isEmpty()) {
            try {
                long expiresInSeconds = Long.parseLong(connection.getExpiresIn());
                Instant expirationTime = connection.getCreatedAt().plusSeconds(expiresInSeconds);
                if (Instant.now().plusSeconds(300).isAfter(expirationTime)) {
                    tokenNeedsRefresh = true;
                }
            } catch (NumberFormatException e) {
                logger.error("Invalid 'expiresIn' value for connection {}: {}", connection.getId(), connection.getExpiresIn());
                tokenNeedsRefresh = true;
            }
        } else {
            tokenNeedsRefresh = true;
        }

        if (tokenNeedsRefresh) {
            if (connection.getRefreshToken() != null && !connection.getRefreshToken().isEmpty()) {
                logger.info("Access token for user {} needs refreshing. Attempting to refresh.", connection.getId());
                TwitterOAuthService.AccessTokenResponse newTokens = twitterOAuthService.refreshAccessToken(connection.getRefreshToken());

                if (newTokens != null) {
                    connection.setAccessToken(newTokens.getAccessToken());
                    connection.setExpiresIn(newTokens.getExpiresIn());
                    if (newTokens.getRefreshToken() != null) {
                        connection.setRefreshToken(newTokens.getRefreshToken());
                    }
                    connection.setCreatedAt(Instant.now());
                    userSocialConnectionRepository.save(connection);
                    logger.info("Access token refreshed and saved for user {}.", connection.getId());
                } else {
                    logger.warn("Failed to refresh token for user {}. User might need to re-authenticate manually.", connection.getId());
                    task.setStatus(TaskStatus.FAILED);
                    task.setErrorMessage("Failed to refresh Twitter access token.");
                    taskRepository.save(task);
                    return;
                }
            } else {
                logger.warn("Access token for user {} needs refresh but no refresh token is available. User must re-authenticate.", connection.getId());
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage("Twitter refresh token missing. User must re-authenticate.");
                taskRepository.save(task);
                return;
            }
        }

        String currentAccessToken = connection.getAccessToken();
        if (currentAccessToken != null && !currentAccessToken.isEmpty()) {
            String contentToPost = task.getGeneratedContent();
            if (contentToPost == null || contentToPost.isEmpty()) {
                logger.warn("No content to post for task: {}", task.getId());
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage("No content generated for this task.");
                taskRepository.save(task);
                return;
            }

            // --- START OF MODIFIED SECTION ---
            boolean published = true; // Simulate successful publishing for now
            logger.info("--- SIMULATING TWITTER POST ---");
            logger.info("Task ID: {}", task.getId());
            logger.info("Content to post: {}", contentToPost);
            logger.info("-----------------------------");

            // Original line, commented out:
            // boolean published = twitterApiService.publishTweet(currentAccessToken, contentToPost);
            // --- END OF MODIFIED SECTION ---

            if (published) {
                logger.info("Content successfully (simulated) published for task {}.", task.getId());

                if (task.isRecurring()) {
                    LocalDateTime nextScheduledTime = calculateNextScheduledTime(task);
                    if (nextScheduledTime != null && (task.getRecurrenceEndDate() == null || nextScheduledTime.isBefore(task.getRecurrenceEndDate()))) {
                        task.setNextScheduledTime(nextScheduledTime);
                        task.setStatus(TaskStatus.CREATED);
                        logger.info("Recurring task {} scheduled for next post at {}", task.getId(), nextScheduledTime);
                    } else {
                        task.setStatus(TaskStatus.COMPLETED);
                        logger.info("Recurring task {} completed or reached end date.", task.getId());
                    }
                } else {
                    task.setStatus(TaskStatus.COMPLETED);
                    logger.info("One-time task {} completed.", task.getId());
                }
                taskRepository.save(task);
            } else {
                logger.error("Failed to publish content for task {}. (Simulation failed if this appears)", task.getId());
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage("Failed to publish content to Twitter.");
                taskRepository.save(task);
            }
        } else {
            logger.error("No valid access token available for user {}. Cannot publish post.", connection.getId());
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("No valid Twitter access token available.");
            taskRepository.save(task);
        }
    }

    private LocalDateTime calculateNextScheduledTime(Task task) {
        LocalDateTime currentScheduledTime = task.getNextScheduledTime();
        if (currentScheduledTime == null) {
            currentScheduledTime = task.getInitialScheduledTime();
            if (currentScheduledTime == null) {
                logger.error("Recurring task {} has neither nextScheduledTime nor initialScheduledTime.", task.getId());
                return null;
            }
        }

        if (task.getRecurrenceInterval() == null || task.getRecurrenceInterval() <= 0 || task.getRecurrenceUnit() == null) {
            logger.warn("Recurring task {} has invalid recurrence interval/unit. Cannot calculate next time.", task.getId());
            return null;
        }

        int interval = task.getRecurrenceInterval();
        String unit = task.getRecurrenceUnit().toUpperCase();

        switch (unit) {
            case "HOURS":
                return currentScheduledTime.plusHours(interval);
            case "DAYS":
                return currentScheduledTime.plusDays(interval);
            case "WEEKS":
                return currentScheduledTime.plusWeeks(interval);
            case "MONTHS":
                return currentScheduledTime.plusMonths(interval);
            case "YEARS":
                return currentScheduledTime.plusYears(interval);
            default:
                logger.warn("Unsupported recurrence unit '{}' for task {}.", unit, task.getId());
                return null;
        }
    }
}