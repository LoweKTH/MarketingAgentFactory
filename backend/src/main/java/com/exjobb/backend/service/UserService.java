package com.exjobb.backend.service;

import com.exjobb.backend.entity.User;
import com.exjobb.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing users and authentication.
 *
 * This service implements Spring Security's UserDetailsService
 * and handles user management operations including registration,
 * authentication, and user administration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Load user by username for Spring Security authentication.
     *
     * This method is required by UserDetailsService and is called
     * by Spring Security during the authentication process.
     *
     * @param username The username to find
     * @return UserDetails object for authentication
     * @throws UsernameNotFoundException if user is not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });
    }

    /**
     * Create a new user account.
     *
     * Registers a new user with encrypted password and default settings.
     * Validates that username and email are unique.
     *
     * @param user The user to create
     * @return The created user
     * @throws IllegalArgumentException if username or email already exists
     */
    @Transactional
    public User createUser(User user) {
        log.info("Creating new user: {}", user.getUsername());

        // Validate username uniqueness
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }

        // Validate email uniqueness
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }

        // Encrypt password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Set default values
        user.setActive(true);
        user.setRole(user.getRole() != null ? user.getRole() : User.Role.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("Successfully created user: {}", savedUser.getUsername());

        return savedUser;
    }

    /**
     * Find user by username.
     *
     * Retrieves a user by their username.
     *
     * @param username The username to search for
     * @return Optional containing the user if found
     */
    public Optional<User> findByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        return userRepository.findByUsername(username);
    }

    /**
     * Find user by email.
     *
     * Retrieves a user by their email address.
     *
     * @param email The email to search for
     * @return Optional containing the user if found
     */
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Update user information.
     *
     * Updates user profile information, excluding password and username
     * which have separate methods for security reasons.
     *
     * @param userId The ID of the user to update
     * @param updatedUser The updated user information
     * @return The updated user
     * @throws IllegalArgumentException if user is not found
     */
    @Transactional
    public User updateUser(Long userId, User updatedUser) {
        log.info("Updating user: {}", userId);

        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Update allowed fields
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setFullName(updatedUser.getFullName());
        existingUser.setPreferences(updatedUser.getPreferences());
        existingUser.setUpdatedAt(LocalDateTime.now());

        // Check email uniqueness if changed
        if (!existingUser.getEmail().equals(updatedUser.getEmail())
                && userRepository.existsByEmail(updatedUser.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + updatedUser.getEmail());
        }

        User savedUser = userRepository.save(existingUser);
        log.info("Successfully updated user: {}", savedUser.getUsername());

        return savedUser;
    }

    /**
     * Change user password.
     *
     * Updates a user's password with proper encryption.
     *
     * @param userId The ID of the user
     * @param newPassword The new password (plain text)
     * @throws IllegalArgumentException if user is not found
     */
    @Transactional
    public void changePassword(Long userId, String newPassword) {
        log.info("Changing password for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("Successfully changed password for user: {}", user.getUsername());
    }

    /**
     * Activate or deactivate a user account.
     *
     * Changes the active status of a user account.
     * Inactive users cannot log in or use the system.
     *
     * @param userId The ID of the user
     * @param active Whether the user should be active
     * @throws IllegalArgumentException if user is not found
     */
    @Transactional
    public void setUserActive(Long userId, boolean active) {
        log.info("Setting user {} active status to: {}", userId, active);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setActive(active);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("Successfully updated active status for user: {}", user.getUsername());
    }

    /**
     * Change user role.
     *
     * Updates a user's role in the system. This affects their
     * permissions and what operations they can perform.
     *
     * @param userId The ID of the user
     * @param newRole The new role to assign
     * @throws IllegalArgumentException if user is not found
     */
    @Transactional
    public void changeUserRole(Long userId, User.Role newRole) {
        log.info("Changing role for user {} to: {}", userId, newRole);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setRole(newRole);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("Successfully changed role for user: {}", user.getUsername());
    }

    /**
     * Get all active users.
     *
     * Retrieves all users that are currently active in the system.
     * Useful for administrative purposes.
     *
     * @return List of active users
     */
    public List<User> getAllActiveUsers() {
        log.debug("Retrieving all active users");

        List<User> users = userRepository.findByActiveTrue();
        log.debug("Found {} active users", users.size());

        return users;
    }

    /**
     * Get users by role.
     *
     * Retrieves all users with a specific role.
     *
     * @param role The role to filter by
     * @return List of users with the specified role
     */
    public List<User> getUsersByRole(User.Role role) {
        log.debug("Retrieving users with role: {}", role);

        List<User> users = userRepository.findByRole(role);
        log.debug("Found {} users with role: {}", users.size(), role);

        return users;
    }

    /**
     * Get user statistics.
     *
     * Returns basic statistics about user accounts for
     * administrative dashboards.
     *
     * @return User statistics object
     */
    public UserStatistics getUserStatistics() {
        log.debug("Calculating user statistics");

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countActiveUsersByRole(User.Role.USER)
                + userRepository.countActiveUsersByRole(User.Role.ADMIN);
        long adminUsers = userRepository.countActiveUsersByRole(User.Role.ADMIN);

        UserStatistics stats = UserStatistics.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .adminUsers(adminUsers)
                .regularUsers(activeUsers - adminUsers)
                .build();

        log.debug("User statistics: {}", stats);
        return stats;
    }

    public boolean authenticate(String username, String password) {
        // 1. Fetch user from the database based on username
        User user = userRepository.findByUsername(username).orElse(null);

        // If user not found, authentication fails
        if (user == null) {
            return false;
        }

        // 2. Compare the provided raw password with the hashed password stored in the database
        // using PasswordEncoder.matches()
        return passwordEncoder.matches(password, user.getPassword());
    }


    /**
     * Simple data class for user statistics.
     */
    @lombok.Data
    @lombok.Builder
    public static class UserStatistics {
        private Long totalUsers;
        private Long activeUsers;
        private Long adminUsers;
        private Long regularUsers;
    }
}