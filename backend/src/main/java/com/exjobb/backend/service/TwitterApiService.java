// src/main/java/com/exjobb/backend/service/TwitterApiService.java
package com.exjobb.backend.service;

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

@Service
public class TwitterApiService {

    private static final Logger logger = LoggerFactory.getLogger(TwitterApiService.class);

    @Value("${twitter.api.tweet-url}") // e.g., https://api.twitter.com/2/tweets
    private String tweetUrl;

    private final RestTemplate restTemplate;

    public TwitterApiService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Publishes a tweet to Twitter using the provided access token.
     *
     * @param accessToken The user's access token for Twitter.
     * @param text The content of the tweet (max 280 characters).
     * @return true if the tweet was published successfully, false otherwise.
     */
    public boolean publishTweet(String accessToken, String text) {
        if (accessToken == null || accessToken.isEmpty()) {
            logger.error("Attempted to publish tweet with null or empty access token.");
            return false;
        }
        if (text == null || text.trim().isEmpty()) {
            logger.warn("Attempted to publish an empty tweet.");
            return false;
        }
        if (text.length() > 280) { // Twitter's standard tweet limit
            logger.warn("Tweet content exceeds 280 characters. Truncating for publication.");
            text = text.substring(0, 280);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken); // Set the Bearer token in the Authorization header
            headers.setContentType(MediaType.APPLICATION_JSON); // Tweets require application/json

            // Twitter API v2 expects a JSON body for posting tweets
            String requestBody = "{\"text\": \"" + text + "\"}"; // Simple JSON for tweet content

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                tweetUrl,
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Tweet published successfully! Response: {}", response.getBody());
                return true;
            } else {
                logger.error("Failed to publish tweet. Status: {}, Body: {}",
                    response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.error("Unauthorized to publish tweet. Access token might be invalid or expired: {}", e.getMessage());
            // This is where your refresh token logic comes into play if you haven't handled it upstream
            return false;
        } catch (Exception e) {
            logger.error("Error publishing tweet: {}", e.getMessage(), e);
            return false;
        }
    }
}