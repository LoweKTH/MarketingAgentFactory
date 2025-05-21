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
 * User entity for the Marketing Agent Factory.
 * Implements Spring Security's UserDetails for authentication and authorization.
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
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique username for login.
     */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * User's email address.
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Encrypted password for authentication.
     */
    @Column(nullable = false)
    private String password;

    /**
     * User's full name for display purposes.
     */
    private String fullName;

    /**
     * User's role in the system.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    /**
     * Whether the user account is active.
     */
    @Builder.Default
    private Boolean active = true;

    /**
     * User preferences for content generation.
     */
    @Column(length = 2000)
    private String preferences;

    /**
     * Timestamp when the user account was created.
     */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the user account was last updated.
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * User roles in the system.
     */
    public enum Role {
        USER,   // Regular user - can generate content
        ADMIN   // Administrator - can manage users and system
    }

    /**
     * Task entities owned by this user.
     */
    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Task> tasks;

    // ================================================
    // Spring Security UserDetails Implementation
    // ================================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active != null && active;
    }
}