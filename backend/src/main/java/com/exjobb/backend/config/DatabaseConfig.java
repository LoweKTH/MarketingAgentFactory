package com.exjobb.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database configuration for the Marketing Agent Factory backend.
 *
 * This configuration class enables JPA features like auditing,
 * repository scanning, and transaction management.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.exjobb.backend.repository")
@EnableJpaAuditing // Enables automatic filling of @CreatedDate and @LastModifiedDate
@EnableTransactionManagement // Enables @Transactional annotations
public class DatabaseConfig {

    /**
     * JPA Auditing configuration is automatically handled by @EnableJpaAuditing.
     *
     * This enables:
     * - @CreatedDate: Automatically sets creation timestamp
     * - @LastModifiedDate: Automatically sets update timestamp
     * - @CreatedBy: Could set who created the entity (if configured)
     * - @LastModifiedBy: Could set who modified the entity (if configured)
     *
     * The timestamps are automatically managed by JPA when entities
     * are saved or updated.
     */
}