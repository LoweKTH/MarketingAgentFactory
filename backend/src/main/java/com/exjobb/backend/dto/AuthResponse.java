package com.exjobb.backend.dto;
import lombok.AllArgsConstructor; // Lombok annotation to generate an all-args constructor
import lombok.Data; // Lombok annotation to generate getters, setters, equals, hashCode, and toString

/**
 * Data Transfer Object (DTO) for handling authentication responses.
 * Used to send the JWT token back to the client after successful login.
 */
@Data // Generates boilerplate code like getters, setters, etc.
@AllArgsConstructor // Generates a constructor with all fields as arguments
public class AuthResponse {
    private String token;
}