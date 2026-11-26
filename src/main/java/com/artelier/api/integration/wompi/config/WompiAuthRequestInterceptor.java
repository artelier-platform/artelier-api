package com.artelier.api.integration.wompi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import feign.RequestInterceptor;
import feign.RequestTemplate;

@Component
public class WompiAuthRequestInterceptor implements RequestInterceptor {

    @Value("${wompi.private-key}")
    private String privateKey;

    @Override
    public void apply(RequestTemplate template) {
        template.header("Authorization", "Bearer " + privateKey);
    }
}