package com.exjobb.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.exjobb.backend.dto.AuthRequest;
import com.exjobb.backend.dto.AuthResponse;
import com.exjobb.backend.service.UserService;
import com.exjobb.backend.utils.JwtUtil;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    @Autowired
    private UserService userService; // Service to handle user authentication logic

    @Autowired
    private JwtUtil jwtUtil; // Utility for JWT token operations

    /**
     * Handles user login requests.
     * Authenticates the user and returns a JWT token upon successful login.
     *
     * @param authRequest Contains the username and password for login.
     * @return ResponseEntity with AuthResponse containing the JWT token if successful,
     * or an error status if authentication fails.
     */
    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequest authRequest) {
        // Authenticate user using the UserService
        boolean isAuthenticated = userService.authenticate(authRequest.getUsername(), authRequest.getPassword());

        if (!isAuthenticated) {
            // If authentication fails, return an unauthorized response
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        // If authentication is successful, generate a JWT token
        final String jwt = jwtUtil.generateToken(authRequest.getUsername());

        // Return the token in the response body
        return ResponseEntity.ok(new AuthResponse(jwt));
    }
}