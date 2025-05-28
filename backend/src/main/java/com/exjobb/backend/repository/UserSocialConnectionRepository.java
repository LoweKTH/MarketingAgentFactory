package com.exjobb.backend.repository;

import com.exjobb.backend.entity.UserSocialConnection;
import com.exjobb.backend.entity.User; // Make sure you import your User entity here
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSocialConnectionRepository extends JpaRepository<UserSocialConnection, Long> {

    // Find a connection by platform and the user's ID on that platform
    Optional<UserSocialConnection> findByPlatformAndPlatformUserId(String platform, String platformUserId);

    // Find all connections for a given platform (useful for scheduled tasks)
    List<UserSocialConnection> findByPlatform(String platform);

    /**
     * Find a social connection by the main application User's ID and the platform.
     * This method assumes that the UserSocialConnection entity has a field named 'user'
     * which is a reference to the User entity.
     *
     * @param userId The ID of the main application user.
     * @param platform The social media platform (e.g., "twitter", "linkedin").
     * @return An Optional containing the UserSocialConnection if found, otherwise empty.
     */
    Optional<UserSocialConnection> findByUserIdAndPlatform(Long userId, String platform);
}