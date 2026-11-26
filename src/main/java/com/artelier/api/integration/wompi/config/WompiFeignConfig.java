package com.artelier.api.integration.wompi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class WompiFeignConfig {

    @Bean
    public RequestInterceptor wompiAuthRequestInterceptor(
            @Value("${wompi.private-key}") String privateKey
    ) {
        return template ->
                template.header(
                        "Authorization",
                        "Bearer " + privateKey
                );
    }

    @Bean
    public ErrorDecoder wompiErrorDecoder(ObjectMapper objectMapper) {
        return new WompiErrorDecoder(objectMapper);
    }
}