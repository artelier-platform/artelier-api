package com.artelier.api.integration.mail.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrevoAuthInterceptor {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Bean
    public RequestInterceptor brevoRequestInterceptor() {
        return (RequestTemplate template) -> {
            template.header("api-key", apiKey);
            template.header("accept", "application/json");
            template.header("content-type", "application/json");
        };
    }
}