package com.artelier.api.integration.wompi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class WompiAuthRequestInterceptor implements RequestInterceptor {

    @Value("${wompi.private-key}")
    private String privateKey;

    @Override
    public void apply(RequestTemplate template) {
        template.header("Authorization", "Bearer " + privateKey);
    }

    @Bean
    public ErrorDecoder wompiErrorDecoder(ObjectMapper objectMapper) {
        return new WompiErrorDecoder(objectMapper);
    }
}