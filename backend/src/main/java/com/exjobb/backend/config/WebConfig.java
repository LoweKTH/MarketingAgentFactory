package com.exjobb.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for HTTP clients.
 * Provides WebClient bean for Python service communication.
 */
@Configuration
public class WebConfig {

    /**
     * WebClient bean for calling the Python service.
     * Used by PythonServiceClient for HTTP requests.
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .build();
    }
}