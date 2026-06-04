package com.artelier.api.integration.wompi.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WompiAcceptanceTokens {
    private String acceptanceToken;
    private String acceptPersonalAuth;
}