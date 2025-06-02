// src/main/java/com/exjobb/backend/service/TwitterOAuthService.java
package com.exjobb.backend.service;

import com.exjobb.backend.entity.UserSocialConnection;
import com.exjobb.backend.repository.UserSocialConnectionRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service("twitterOAuthService")
public class TwitterOAuthService implements OAuthService {

    private static final Logger logger = LoggerFactory.getLogger(TwitterOAuthService.class);

    @Value("${twitter.client.id}")
    private String clientId;

    @Value("${twitter.client.secret}")
    private String clientSecret;

    @Value("${twitter.redirect.uri}")
    private String redirectUri;

    @Value("${twitter.oauth.token-url}")
    private String tokenUrl;

    @Value("${twitter.api.users-me-url}")
    private String usersMeUrl;

    @Value("${twitter.api.tweets-url}")
    private String tweetsUrl;

    private final RestTemplate restTemplate;
    private final UserSocialConnectionRepository userSocialConnectionRepository;
    private final ObjectMapper objectMapper;

    private final String codeChallenge = "challenge";
    private final String codeChallengeMethod = "plain";

    public TwitterOAuthService(UserSocialConnectionRepository userSocialConnectionRepository) {
        this.restTemplate = new RestTemplate();
        this.userSocialConnectionRepository = userSocialConnectionRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String generateAuthUrl(String state) {
        try {
            String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString());
            String scope = URLEncoder.encode("tweet.read tweet.write users.read offline.access",
                    StandardCharsets.UTF_8.toString());

            return "https://x.com/i/oauth2/authorize"
                    + "?response_type=code"
                    + "&client_id=" + clientId
                    + "&redirect_uri=" + encodedRedirectUri
                    + "&scope=" + scope
                    + "&state=" + state
                    + "&code_challenge=" + codeChallenge
                    + "&code_challenge_method=" + codeChallengeMethod;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Twitter auth URL", e);
        }
    }

    public String handleCallback(String code, String state, String error, String error_description, HttpSession session,
            String frontendUrl) {
        String frontendRedirectBaseUrl = frontendUrl + "/profile";

        logger.info("All session attributes in service: ");
        session.getAttributeNames().asIterator()
                .forEachRemaining(attr -> logger.info("   {}: {}", attr, session.getAttribute(attr)));

        if (error != null) {
            logger.error("Twitter OAuth error: {} - {}", error, error_description);
            String errorRedirectUrl = frontendRedirectBaseUrl + "?error="
                    + URLEncoder.encode(error, StandardCharsets.UTF_8);
            if (error_description != null) {
                errorRedirectUrl += "&error_description="
                        + URLEncoder.encode(error_description, StandardCharsets.UTF_8);
            }
            return errorRedirectUrl;
        }

        String expectedState = (String) session.getAttribute("oauthState_twitter");
        logger.info("Expected state: '{}', Received state: '{}'", expectedState, state);

        if (expectedState == null || !expectedState.equals(state)) {
            logger.error("Invalid state parameter. Expected: '{}', Received: '{}'", expectedState, state);
            String errorRedirectUrl = frontendRedirectBaseUrl + "?error=invalid_state&error_description=" +
                    URLEncoder.encode("Invalid state parameter - Expected: " + expectedState + ", Received: " + state,
                            StandardCharsets.UTF_8);
            return errorRedirectUrl;
        }

        if (code == null || code.isEmpty()) {
            logger.error("No authorization code received");
            String errorRedirectUrl = frontendRedirectBaseUrl + "?error=no_code&error_description=" +
                    URLEncoder.encode("No authorization code received", StandardCharsets.UTF_8);
            return errorRedirectUrl;
        }

        session.removeAttribute("oauthState_twitter");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String auth = clientId + ":" + clientSecret;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + new String(encodedAuth, StandardCharsets.UTF_8);
            headers.set("Authorization", authHeader);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("code", code);
            body.add("grant_type", "authorization_code");
            body.add("client_id", clientId);
            body.add("redirect_uri", redirectUri);
            body.add("code_verifier", codeChallenge);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );
            System.out.println("test1: " + responseEntity);

            AccessTokenResponse tokenResponse = null;

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    tokenResponse = objectMapper.readValue(responseEntity.getBody(), AccessTokenResponse.class);
                } catch (Exception e) {
                    logger.error("Failed to parse access token response: {}", e.getMessage(), e);
                    return frontendRedirectBaseUrl + "?error=json_parse_failed&error_description=" +
                            URLEncoder.encode("Failed to parse access token response", StandardCharsets.UTF_8);
                }
            } else {
                logger.error("Failed to obtain access token. Status: {}, Body: {}",
                        responseEntity.getStatusCode(), responseEntity.getBody());
                return frontendRedirectBaseUrl + "?error=token_exchange_failed&error_description=" +
                        URLEncoder.encode("Failed to exchange authorization code for access token. " +
                                "Server responded with status: " + responseEntity.getStatusCode(),
                                StandardCharsets.UTF_8);
            }

            if (tokenResponse != null && tokenResponse.getAccessToken() != null) {
                logger.info("Successfully obtained Twitter access token!");
                logger.debug("Access Token: {}", tokenResponse.getAccessToken());

                String twitterUserId = fetchTwitterUserId(tokenResponse.getAccessToken());
                if (twitterUserId == null) {
                    logger.error("Failed to fetch Twitter user ID after successful token exchange.");
                    return frontendRedirectBaseUrl + "?error=twitter_user_id_fetch_failed&error_description=" +
                            URLEncoder.encode("Could not retrieve Twitter user ID", StandardCharsets.UTF_8);
                }

                // Assuming you have a way to identify your internal user (e.g., from session or security context)
                Long currentUserId = 1L; // Replace with actual user ID retrieval from your auth system

                Optional<UserSocialConnection> existingConnection = userSocialConnectionRepository.findByPlatformAndPlatformUserId("twitter", twitterUserId);

                UserSocialConnection connection;
                if (existingConnection.isPresent()) {
                    connection = existingConnection.get();
                    logger.info("Updating existing Twitter connection for user ID: {}", twitterUserId);
                } else {
                    connection = new UserSocialConnection();
                    connection.setPlatform("twitter");
                    connection.setPlatformUserId(twitterUserId);
                    connection.setCreatedAt(Instant.now());
                    logger.info("Creating new Twitter connection for user ID: {}", twitterUserId);
                }

                connection.setAccessToken(tokenResponse.getAccessToken());
                connection.setRefreshToken(tokenResponse.getRefreshToken());
                connection.setExpiresIn(tokenResponse.getExpiresIn());
                connection.setScope(tokenResponse.getScope());
                connection.setTokenType(tokenResponse.getTokenType());

                userSocialConnectionRepository.save(connection);
                logger.info("Twitter access token and user ID saved/updated in database.");

                return frontendRedirectBaseUrl + "?success=true&platform=twitter";

            } else {
                logger.error("Failed to obtain access token: {}", tokenResponse);
                return frontendRedirectBaseUrl + "?error=token_exchange_failed&error_description=" +
                        URLEncoder.encode("Failed to exchange authorization code for access token",
                                StandardCharsets.UTF_8);
            }

        } catch (Exception e) {
            logger.error("Error during access token exchange or saving: {}", e.getMessage(), e);
            return frontendRedirectBaseUrl + "?error=token_exchange_exception&error_description=" +
                    URLEncoder.encode("An error occurred during access token exchange: " + e.getMessage(),
                            StandardCharsets.UTF_8);
        }
    }

    private String fetchTwitterUserId(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    usersMeUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                if (responseMap.containsKey("data")) {
                    Map<String, String> userData = (Map<String, String>) responseMap.get("data");
                    if (userData.containsKey("id")) {
                        String userId = userData.get("id");
                        logger.info("Successfully fetched Twitter user ID: {}", userId);
                        return userId;
                    }
                }
            }
            logger.error("Failed to fetch Twitter user ID. Status: {}, Body: {}",
                    response.getStatusCode(), response.getBody());
            return null;
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.error("Unauthorized to fetch Twitter user ID. Access token might be invalid or expired: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Error fetching Twitter user ID: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Refreshes the Twitter access token using the provided refresh token.
     *
     * @param currentRefreshToken The refresh token obtained during initial OAuth.
     * @return A new AccessTokenResponse if successful, or null if refresh fails.
     */
    public AccessTokenResponse refreshAccessToken(String currentRefreshToken) {
        logger.info("Attempting to refresh Twitter access token using refresh token.");
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String auth = clientId + ":" + clientSecret;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + new String(encodedAuth, StandardCharsets.UTF_8);
            headers.set("Authorization", authHeader);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("refresh_token", currentRefreshToken);
            body.add("client_id", clientId); // Required for public clients and sometimes for confidential clients

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                AccessTokenResponse newTokenResponse = objectMapper.readValue(responseEntity.getBody(), AccessTokenResponse.class);
                logger.info("Successfully refreshed Twitter access token.");
                return newTokenResponse;
            } else {
                logger.error("Failed to refresh access token. Status: {}, Body: {}",
                        responseEntity.getStatusCode(), responseEntity.getBody());
                return null;
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.error("Refresh token is invalid or expired (Unauthorized): {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Error during access token refresh: {}", e.getMessage(), e);
            return null;
        }
        
    }

    public String postTweet(String accessToken, String tweetContent) {
        logger.info("Attempting to publish tweet to Twitter.");
        try {
            HttpHeaders headers = new HttpHeaders();

            
            System.out.println(accessToken);
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON); // Twitter expects JSON for posting tweets

            // Twitter API v2 endpoint for posting tweets
            // https://developer.twitter.com/en/docs/twitter-api/tweets/manage-tweets/api-reference/post-tweets
            // The JSON body should be {"text": "Your tweet content"}
            Map<String, String> requestBody = Collections.singletonMap("text", tweetContent);
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    tweetsUrl, // Use the new @Value field for the tweets API URL
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                // Parse the response to extract the tweet ID if needed
                Map<String, Object> responseMap = objectMapper.readValue(responseEntity.getBody(), Map.class);
                if (responseMap.containsKey("data") && ((Map<String, String>) responseMap.get("data")).containsKey("id")) {
                    String tweetId = ((Map<String, String>) responseMap.get("data")).get("id");
                    logger.info("Tweet published successfully. Tweet ID: {}", tweetId);
                    return tweetId;
                } else {
                    logger.warn("Tweet published successfully but could not retrieve tweet ID from response: {}", responseEntity.getBody());
                    return "UNKNOWN_ID"; // Return a placeholder or handle as appropriate
                }
            } else {
                logger.error("Failed to publish tweet. Status: {}, Body: {}",
                        responseEntity.getStatusCode(), responseEntity.getBody());
                throw new RuntimeException("Failed to publish tweet: " + responseEntity.getBody());
            }
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error publishing tweet: Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("HTTP error publishing tweet: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            logger.error("An unexpected error occurred while publishing tweet: {}", e.getMessage(), e);
            throw new RuntimeException("An unexpected error occurred during tweet publication.", e);
        }
    }

    

    public static class AccessTokenResponse {
        @JsonProperty("token_type")
        private String tokenType;
        @JsonProperty("expires_in")
        private String expiresIn;
        @JsonProperty("access_token")
        private String accessToken;

        private String scope;

        @JsonProperty("refresh_token")
        private String refreshToken;

        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }

        public String getExpiresIn() { return expiresIn; }
        public void setExpiresIn(String expiresIn) { this.expiresIn = expiresIn; }

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

        @Override
        public String toString() {
            return "AccessTokenResponse{" +
                    "tokenType='" + tokenType + '\'' +
                    ", expiresIn='" + expiresIn + '\'' +
                    ", accessToken='" + (accessToken != null ? "[REDACTED]" : "null") + '\'' +
                    ", scope='" + scope + '\'' +
                    ", refreshToken='" + (refreshToken != null ? "[REDACTED]" : "null") + '\'' +
                    '}';
        }
    }
}