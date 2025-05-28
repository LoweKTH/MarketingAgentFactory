// src/main/java/com/exjobb/backend/repository/UserSocialConnectionRepository.java
package com.exjobb.backend.repository;

import com.exjobb.backend.entity.UserSocialConnection;
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

    // If you have a User entity, you might also add:
    // Optional<UserSocialConnection> findByUserAndPlatform(User user, String platform);
    // List<UserSocialConnection> findByUser(User user);
}