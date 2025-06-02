// src/main/java/com/exjobb/backend/service/SocialConnectionService.java
package com.exjobb.backend.service;

import com.exjobb.backend.entity.UserSocialConnection;
import com.exjobb.backend.repository.UserSocialConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SocialConnectionService {
 
    private static final Logger logger = LoggerFactory.getLogger(SocialConnectionService.class);

    private final UserSocialConnectionRepository userSocialConnectionRepository;
    private final TwitterOAuthService twitterOAuthService; // Inject the Twitter specific service

    // You can add other OAuth services here as needed, e.g., LinkedInOAuthService, FacebookOAuthService

    public SocialConnectionService(UserSocialConnectionRepository userSocialConnectionRepository,
                                   TwitterOAuthService twitterOAuthService) {
        this.userSocialConnectionRepository = userSocialConnectionRepository;
        this.twitterOAuthService = twitterOAuthService;
    }

    /**
     * Fetches all social connections, validates their access tokens,
     * and refreshes them if necessary.
     * This method currently ignores the 'userId' and fetches all connections for simplicity.
     *
     * @param userId (Ignored for now, but kept for method signature consistency if you change your mind later).
     * @return A map where keys are platform names (e.g., "twitter") and values are boolean
     * indicating if the connection is currently valid. Note: This map will aggregate
     * status across all connections if multiple users have the same platform.
     */
    public Map<String, Boolean> getAndValidateConnectedPlatforms(Long userId) { // userId parameter is currently ignored
        List<UserSocialConnection> connections = userSocialConnectionRepository.findAll(); // Fetch ALL connections
        Map<String, Boolean> connectedStatus = new HashMap<>();

        // Initialize all supported platforms as not connected
        // This map will reflect if *any* connection for a platform is valid.
        connectedStatus.put("twitter", false);
        connectedStatus.put("linkedin", false);
        connectedStatus.put("facebook", false);
        connectedStatus.put("instagram", false);

        for (UserSocialConnection connection : connections) {
            String platform = connection.getPlatform().toLowerCase();
            UserSocialConnection validatedConnection = null;

            switch (platform) {
                case "twitter":
                    validatedConnection = ensureAccessTokenValid(connection); // Use the private helper
                    break;
                // Add cases for other platforms here, delegating to their respective refresh methods
                // case "linkedin":
                //     validatedConnection = ensureAccessTokenValidForLinkedIn(connection);
                //     break;
                // case "facebook":
                //     validatedConnection = ensureAccessTokenValidForFacebook(connection);
                //     break;
                default:
                    logger.warn("Unknown platform encountered: {}", platform);
                    break;
            }

            // A connection is considered valid if validatedConnection is not null and has an accessToken.
            // If any connection for a platform is valid, set its status to true.
            if (validatedConnection != null && validatedConnection.getAccessToken() != null && !validatedConnection.getAccessToken().isEmpty()) {
                connectedStatus.put(platform, true);
            }
        }

        return connectedStatus;
    }

    /**
     * Finds a UserSocialConnection by platform and platformUserId,
     * ensuring the token is valid before returning.
     * This is useful if you need to retrieve a specific connection for an operation (e.g., posting).
     *
     * @param platform The social media platform.
     * @param platformUserId The user ID on that platform.
     * @return An Optional containing the validated UserSocialConnection, or empty if not found/invalid.
     */
    public Optional<UserSocialConnection> findAndValidateConnection(String platform, String platformUserId) {
        Optional<UserSocialConnection> connectionOpt = userSocialConnectionRepository.findByPlatformAndPlatformUserId(platform, platformUserId);
        if (connectionOpt.isEmpty()) {
            return Optional.empty();
        }

        UserSocialConnection connection = connectionOpt.get();
        UserSocialConnection validatedConnection = null;

        switch (platform.toLowerCase()) {
            case "twitter":
                validatedConnection = ensureAccessTokenValid(connection); // Use the private helper
                break;
            // Add cases for other platforms here
            default:
                logger.warn("Attempted to find and validate unknown platform: {}", platform);
                break;
        }

        if (validatedConnection != null && validatedConnection.getAccessToken() != null && !validatedConnection.getAccessToken().isEmpty()) {
            return Optional.of(validatedConnection);
        } else {
            return Optional.empty(); // Token is invalid or refresh failed
        }
    }

    /**
     * Saves or updates a UserSocialConnection. This method can be called
     * after a successful OAuth callback to persist new or refreshed tokens.
     *
     * @param connection The UserSocialConnection entity to save.
     * @return The saved UserSocialConnection entity.
     */
    public UserSocialConnection saveConnection(UserSocialConnection connection) {
        return userSocialConnectionRepository.save(connection);
    }

    /**
     * Private helper method to ensure a given UserSocialConnection's access token is valid.
     * It checks expiration and attempts to refresh if needed.
     *
     * @param connection The UserSocialConnection to validate.
     * @return The updated/refreshed UserSocialConnection, or null if validation/refresh failed.
     */
    private UserSocialConnection ensureAccessTokenValid(UserSocialConnection connection) {
        if (!"twitter".equalsIgnoreCase(connection.getPlatform())) {
            logger.warn("ensureAccessTokenValid called with non-Twitter platform: {}", connection.getPlatform());
            return null; // This method is specifically for Twitter via twitterOAuthService
        }

        // Calculate expiration time using the token's 'createdAt' (when it was last issued/refreshed)
        // and 'expiresIn'
        Instant tokenIssuedAt = connection.getCreatedAt();
        long expiresInSeconds = 0;
        try {
            expiresInSeconds = Long.parseLong(connection.getExpiresIn());
        } catch (NumberFormatException e) {
            logger.error("Invalid expiresIn format for Twitter connection {}: {}", connection.getId(), connection.getExpiresIn());
            // Invalidate the token if expiresIn is malformed, as we can't reliably validate it
            connection.setAccessToken(null);
            connection.setRefreshToken(null);
            userSocialConnectionRepository.save(connection);
            return null;
        }

        Instant expirationTime = tokenIssuedAt.plusSeconds(expiresInSeconds);
        Instant now = Instant.now();

        // Add a buffer (e.g., 5 minutes) to refresh before it actually expires
        long refreshBufferSeconds = 300; // 5 minutes

        if (now.isAfter(expirationTime.minusSeconds(refreshBufferSeconds))) {
            logger.info("Twitter access token for user 1 (platform user ID: {}) is expired or nearing expiration. Attempting to refresh...",
                     connection.getPlatformUserId());

            if (connection.getRefreshToken() == null || connection.getRefreshToken().isEmpty()) {
                logger.warn("No refresh token available for Twitter connection {}. Cannot refresh. User ID: 1",
                        connection.getId());
                connection.setAccessToken(null);
                userSocialConnectionRepository.save(connection);
                return null;
            }

            TwitterOAuthService.AccessTokenResponse newTokenResponse =
                    twitterOAuthService.refreshAccessToken(connection.getRefreshToken());

            if (newTokenResponse != null && newTokenResponse.getAccessToken() != null) {
                connection.setAccessToken(newTokenResponse.getAccessToken());
                if (newTokenResponse.getRefreshToken() != null && !newTokenResponse.getRefreshToken().isEmpty()) {
                    connection.setRefreshToken(newTokenResponse.getRefreshToken());
                }
                connection.setExpiresIn(newTokenResponse.getExpiresIn());
                connection.setScope(newTokenResponse.getScope());
                connection.setTokenType(newTokenResponse.getTokenType());
                connection.setCreatedAt(Instant.now()); // CRUCIAL: Update createdAt to reflect the new token's issuance time
                userSocialConnectionRepository.save(connection);
                logger.info("Twitter access token for user 1 refreshed successfully.");
                return connection;
            } else {
                logger.error("Failed to refresh Twitter access token for user 1. Invalidating connection.");
                connection.setAccessToken(null);
                connection.setRefreshToken(null);
                userSocialConnectionRepository.save(connection);
                return null;
            }
        } else {
            logger.debug("Twitter access token for user 1 is still valid.");
            return connection;
        }
    }  
    
} 