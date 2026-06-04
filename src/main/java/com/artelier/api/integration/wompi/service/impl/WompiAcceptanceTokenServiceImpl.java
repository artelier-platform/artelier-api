package com.artelier.api.integration.wompi.service.impl;

import com.artelier.api.integration.wompi.dto.request.WompiAcceptanceTokens;
import com.artelier.api.integration.wompi.dto.response.WompiMerchantResponse;
import com.artelier.api.integration.wompi.service.WompiAcceptanceTokenService;
import com.artelier.api.integration.wompi.service.WompiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WompiAcceptanceTokenServiceImpl implements WompiAcceptanceTokenService {

    private final WompiClient wompiClient;

    @Value("${wompi.public-key}")
    private String publicKey;

    @Override
    @Cacheable("wompiTokens")
    public WompiAcceptanceTokens getTokens() {
        WompiMerchantResponse merchant = wompiClient.getMerchant(publicKey);

        String acceptanceToken = merchant.getData()
                .getPresignedAcceptance()
                .getAcceptanceToken();

        String acceptPersonalAuth = merchant.getData()
                .getPresignedPersonalDataAuth()
                .getAcceptanceToken();

        return new WompiAcceptanceTokens(acceptanceToken, acceptPersonalAuth);
    }
}