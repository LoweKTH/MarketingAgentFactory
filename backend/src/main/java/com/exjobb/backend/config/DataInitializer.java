package com.exjobb.backend.config;

import com.exjobb.backend.entity.User;
import com.exjobb.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Database initialization for development testing.
 * Creates test data for the application.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Initialize database with test data.
     */
    @Bean
    public CommandLineRunner initializeDatabase() {
        return args -> {
            // Check if we already have users
            if (userRepository.count() == 0) {
                createTestUser();
            }
        };
    }

    /**
     * Create a test user for development.
     */
    private void createTestUser() {
        log.info("Creating test user...");

        User testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password"))
                .fullName("Test User")
                .role(User.Role.ADMIN)
                .active(true)
                .build();

        userRepository.save(testUser);

        log.info("Test user created with ID: {}", testUser.getId());
    }
}