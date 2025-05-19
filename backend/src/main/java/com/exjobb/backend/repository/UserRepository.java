package com.exjobb.backend.repository;

import com.exjobb.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity operations.
 *
 * Provides data access methods for user management and authentication.
 * Extends JpaRepository to get standard CRUD operations plus custom queries
 * for user administration and security features.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their username.
     * Used primarily for authentication by Spring Security.
     *
     * @param username The username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by their email address.
     * Useful for password reset and user lookup functionality.
     *
     * @param email The email address to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a username already exists.
     * Used during user registration to ensure uniqueness.
     *
     * @param username The username to check
     * @return True if username exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Check if an email address already exists.
     * Used during user registration to ensure uniqueness.
     *
     * @param email The email address to check
     * @return True if email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find all active users.
     * Useful for user management and administration.
     *
     * @return List of all active users
     */
    List<User> findByActiveTrue();

    /**
     * Find users by role.
     * Useful for administrative functions and role management.
     *
     * @param role The role to filter by
     * @return List of users with the specified role
     */
    List<User> findByRole(User.Role role);

    /**
     * Find users created within a specific date range.
     * Useful for reporting and user growth analytics.
     *
     * @param start Start date (inclusive)
     * @param end End date (inclusive)
     * @return List of users created within the date range
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :start AND :end ORDER BY u.createdAt DESC")
    List<User> findUsersCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count active users by role.
     * Provides statistics for administrative dashboards.
     *
     * @param role The role to count
     * @return Number of active users with the specified role
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.active = true")
    long countActiveUsersByRole(@Param("role") User.Role role);

    /**
     * Find users who haven't been active recently.
     * Helps identify users who might need re-engagement.
     *
     * @param lastActiveDate Users who haven't been updated since this date
     * @return List of potentially inactive users
     */
    @Query("SELECT u FROM User u WHERE u.active = true AND u.updatedAt < :lastActiveDate")
    List<User> findInactiveUsers(@Param("lastActiveDate") LocalDateTime lastActiveDate);
}