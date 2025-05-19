package com.exjobb.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Entity representing a user in the Marketing Agent Factory system.
 *
 * This entity implements Spring Security's UserDetails interface
 * to provide authentication and authorization capabilities.
 * It stores user account information and preferences.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User implements UserDetails {

    /**
     * Unique identifier for the user.
     * Generated automatically by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique username for login.
     * Used for authentication purposes.
     */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * User's email address.
     * Must be unique and is used for notifications.
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Encrypted password for authentication.
     * Stored as BCrypt hash for security.
     */
    @Column(nullable = false)
    private String password;

    /**
     * User's full name for display purposes.
     * Used in the UI for personalization.
     */
    private String fullName;

    /**
     * User's role in the system.
     * Values: USER, ADMIN
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    /**
     * Whether the user account is active.
     * Inactive users cannot log in or use the system.
     */
    @Builder.Default
    private Boolean active = true;

    /**
     * User preferences stored as JSON string.
     * Contains settings like default brand voice, platform preferences, etc.
     */
    @Column(length = 2000)
    private String preferences;

    /**
     * Timestamp when the user account was created.
     * Automatically set by JPA auditing.
     */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the user account was last updated.
     * Automatically updated by JPA auditing.
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Enum defining user roles in the system.
     * Determines what operations a user can perform.
     */
    public enum Role {
        USER,   // Regular user - can generate content
        ADMIN   // Administrator - can manage users and system settings
    }

    // ================================================
    // UserDetails Interface Implementation
    // These methods are required by Spring Security
    // ================================================

    /**
     * Returns the authorities granted to the user.
     * Based on the user's role.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Returns the username used for authentication.
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Indicates whether the user's account has expired.
     * We keep accounts active indefinitely.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is locked or unlocked.
     * We don't implement account locking in this MVP.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials (password) has expired.
     * We don't implement password expiration in this MVP.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled or disabled.
     * Based on the 'active' field.
     */
    @Override
    public boolean isEnabled() {
        return active;
    }
}