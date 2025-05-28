// src/main/java/com/exjobb/backend/model/UserSocialConnection.java
package com.exjobb.backend.entity;

import jakarta.persistence.*; // Ensure you use jakarta.persistence for Spring Boot 3+
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant; // For storing the token creation timestamp

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_social_connections")
public class UserSocialConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to your User entity (assuming you have one).
    // If you have a User entity, uncomment the following and ensure 'User' class exists.
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "user_id", nullable = false)
    // private User user; // This links a social connection to a specific internal user

    @Column(nullable = false)
    private String platform; // e.g., "twitter", "linkedin", "facebook"

    @Column(nullable = false, length = 1000) // Access tokens can be quite long
    private String accessToken;

    @Column(length = 1000) // Refresh tokens are optional, but can also be long
    private String refreshToken; // Nullable if the platform doesn't provide one or it's not needed

    private String expiresIn; // Duration in seconds until the access token expires (as a String from API)
    private String scope;
    private String tokenType; // e.g., "bearer"

    @Column(nullable = false)
    private String platformUserId; // The unique ID of the user on the social platform (e.g., Twitter's user ID)

    @Column(nullable = false)
    private Instant createdAt; // Timestamp when the token was first issued or last refreshed

    
}