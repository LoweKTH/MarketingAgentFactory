package com.exjobb.backend.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean  // 🔌 Gör WebClient.Builder till en hanterad bean så den kan injiceras där den behövs
    public WebClient.Builder webClientBuilder(){
        return WebClient.builder();
    }
}
