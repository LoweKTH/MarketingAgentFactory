package com.exjobb.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

/**
 * Web configuration for the Marketing Agent Factory backend.
 *
 * This configuration class sets up CORS policies, WebClient for
 * communicating with the Python service, and other web-related settings.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${python.service.url:http://localhost:5000}")
    private String pythonServiceUrl;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Configure CORS (Cross-Origin Resource Sharing) settings.
     *
     * Allows the frontend to communicate with the backend API
     * from a different origin (different port/domain).
     * This is essential for development when frontend and backend
     * run on different ports.
     *
     * @param registry CORS registry to configure
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // Allow requests from the frontend URL
                .allowedOrigins(frontendUrl, "http://localhost:3000", "http://localhost:3001")
                // Allow common HTTP methods
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // Allow common headers
                .allowedHeaders("*")
                // Allow credentials (cookies, authorization headers)
                .allowCredentials(true)
                // Cache preflight requests for 1 hour
                .maxAge(3600);
    }

    /**
     * Configure WebClient for communicating with the Python service.
     *
     * WebClient is Spring's reactive HTTP client, used to send
     * requests to the Python Content Agent service.
     *
     * @return Configured WebClient instance
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                // Set base URL for Python service
                .baseUrl(pythonServiceUrl)
                // Configure default headers
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("User-Agent", "MarketingAgentFactory-Backend/1.0")
                // Configure timeouts
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(1024 * 1024)) // 1MB max response size
                // Build the WebClient
                .build();
    }

    /**
     * Configure WebClient with custom timeout settings.
     *
     * This bean provides a WebClient specifically for long-running
     * operations like content generation that might take longer.
     *
     * @return WebClient configured for long operations
     */
    @Bean("longTimeoutWebClient")
    public WebClient longTimeoutWebClient() {
        return WebClient.builder()
                .baseUrl(pythonServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("User-Agent", "MarketingAgentFactory-Backend/1.0")
                // Configure for longer operations
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024)) // 2MB for larger responses
                .build();
    }
}