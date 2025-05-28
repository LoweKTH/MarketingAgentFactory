// src/main/java/com/exjobb/backend/controller/OAuthController.java
package com.exjobb.backend.controller;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.exjobb.backend.service.OAuthService;
import com.exjobb.backend.service.TwitterOAuthService;
import com.exjobb.backend.entity.UserSocialConnection;
import com.exjobb.backend.repository.UserSocialConnectionRepository; // Import repository

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap; // For building the response map
import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth/")
@CrossOrigin(origins = "http://localhost:5173")
public class OAuthController {

    private final Map<String, OAuthService> oauthServices;
    private final TwitterOAuthService twitterOAuthService;
    private final UserSocialConnectionRepository userSocialConnectionRepository; // Inject repository

    private static final Logger logger = LoggerFactory.getLogger(OAuthController.class);
    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // Modified constructor to include UserSocialConnectionRepository
    public OAuthController(Map<String, OAuthService> oauthServices, TwitterOAuthService twitterOAuthService,
                           UserSocialConnectionRepository userSocialConnectionRepository) {
        this.oauthServices = oauthServices;
        this.twitterOAuthService = twitterOAuthService;
        this.userSocialConnectionRepository = userSocialConnectionRepository; // Initialize
    }

    @GetMapping("/{platform}/initiate")
    public ResponseEntity<Map<String, String>> initiateOAuth(@PathVariable String platform, HttpSession session) {
        logger.info("Initiating OAuth for platform: {}", platform);
        logger.info("Session ID at initiate: {}", session.getId());

        String state = generateState();
        String sessionKey = "oauthState_" + platform.toLowerCase();
        session.setAttribute(sessionKey, state);
        logger.info("Generated state: '{}', stored with key: '{}'", state, sessionKey);
        logger.info("Session attribute set: {}", session.getAttribute(sessionKey));

        OAuthService service = oauthServices.get(platform.toLowerCase() + "OAuthService");
        if (service == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unsupported platform: " + platform));
        }

        String redirectUrl = service.generateAuthUrl(state);
        logger.info("Generated redirect URL: {}", redirectUrl);

        return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));
    }

    @GetMapping("/twitter/callback")
    public ResponseEntity<?> handleTwitterCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description,
            HttpSession session) {

        logger.info("Twitter callback received - code: {}, state: {}, error: {}",
                   code != null ? "present" : "null", state, error);
        logger.info("Session ID: {}", session.getId());

        String redirectUrl = twitterOAuthService.handleCallback(code, state, error, error_description, session, frontendUrl);
        return ResponseEntity.status(302)
            .header("Location", redirectUrl)
            .build();
    }

    /**
     * New Endpoint: Get connected platforms for the current user.
     * In a real application, you'd get the user ID from the authenticated context (e.g., Spring Security).
     * For this example, we'll assume a dummy user ID or just return all connections.
     */
    @GetMapping("/connections")
    public ResponseEntity<Map<String, Boolean>> getConnections() {
        // Replace with actual user ID retrieval from your authentication system
        // For demonstration, let's assume we want to check all connected platforms for now,
        // or filter by a specific user ID if you have one.
        // Long currentUserId = ... // Get from session, Spring Security, etc.

        Map<String, Boolean> connectedPlatforms = new HashMap<>();
        connectedPlatforms.put("twitter", false);
        connectedPlatforms.put("linkedin", false);
        connectedPlatforms.put("facebook", false);
        connectedPlatforms.put("instagram", false);

        // Find all connections (for simplicity, assuming one user for now, or you'd filter by user ID)
        List<UserSocialConnection> connections = userSocialConnectionRepository.findAll();
        // If you had a user, you'd do: userSocialConnectionRepository.findByUser(user);

        for (UserSocialConnection connection : connections) {
            connectedPlatforms.put(connection.getPlatform().toLowerCase(), true);
        }

        return ResponseEntity.ok(connectedPlatforms);
    }


    private String generateState() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}