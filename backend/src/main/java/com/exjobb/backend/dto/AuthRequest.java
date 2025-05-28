package com.exjobb.backend.dto;

import lombok.Data; // Lombok annotation to generate getters, setters, equals, hashCode, and toString

/**
 * Data Transfer Object (DTO) for handling authentication requests.
 * Used to capture username and password from the login request body.
 */
@Data // Generates boilerplate code like getters, setters, etc.
public class AuthRequest {
    private String username;
    private String password;
}