package com.exjobb.backend.service;

public interface OAuthService {
    String generateAuthUrl(String state);
    //String exchangeCodeForAccessToken(String code);
    // Add other common methods like getUserProfile, postContent, etc.
    // Map<String, Object> getUserProfile(String accessToken);
    // Map<String, Object> postContent(String accessToken, String message);
}
