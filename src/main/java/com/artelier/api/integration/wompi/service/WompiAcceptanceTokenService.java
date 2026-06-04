package com.artelier.api.integration.wompi.service;


import com.artelier.api.integration.wompi.dto.request.WompiAcceptanceTokens;

public interface WompiAcceptanceTokenService {
    WompiAcceptanceTokens getTokens();
}