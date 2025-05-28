package com.exjobb.backend.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility class for generating and validating JSON Web Tokens (JWTs).
 */
@Component
public class JwtUtil {

    // Secret key for signing the JWT. Loaded from application.properties.
    @Value("${jwt.secret:yourSecretKey}") // Default value for development
    private String secret;

    // Expiration time for the JWT in milliseconds. Loaded from application.properties.
    @Value("${jwt.expiration:3600000}") // Default: 1 hour (3600 * 1000 ms)
    private long expiration;

    /**
     * Generates a JWT token for a given username.
     *
     * @param username The subject (username) for whom the token is generated.
     * @return The generated JWT string.
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>(); // Claims to be included in the JWT payload
        return createToken(claims, username);
    }

    /**
     * Helper method to create the JWT token.
     *
     * @param claims Custom claims to be added to the token.
     * @param subject The subject of the token (e.g., username).
     * @return The JWT string.
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims) // Set custom claims
                .setSubject(subject) // Set the subject (username)
                .setIssuedAt(new Date(System.currentTimeMillis())) // Set issue date
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // Set expiration date
                .signWith(SignatureAlgorithm.HS256, secret) // Sign the token with HS256 algorithm and secret key
                .compact(); // Build and compact the JWT to a string
    }

    // In a real application, you would also add methods for:
    // - validateToken(String token, UserDetails userDetails)
    // - extractUsername(String token)
    // - isTokenExpired(String token)
    // For this basic login example, we only need token generation.
}