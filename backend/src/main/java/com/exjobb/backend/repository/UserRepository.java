package com.exjobb.backend.repository;

import com.exjobb.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by username.
     *
     * @param username The username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by email.
     *
     * @param email The email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user with the given username exists.
     *
     * @param username The username to check
     * @return True if a user with the username exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Check if a user with the given email exists.
     *
     * @param email The email to check
     * @return True if a user with the email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find all active users.
     *
     * @return List of active users
     */
    List<User> findByActiveTrue();

    /**
     * Find all users with a specific role.
     *
     * @param role The role to search for
     * @return List of users with the specified role
     */
    List<User> findByRole(User.Role role);

    /**
     * Count the number of active users with a specific role.
     *
     * @param role The role to count
     * @return The count of active users with the specified role
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true AND u.role = :role")
    long countActiveUsersByRole(@Param("role") User.Role role);
}