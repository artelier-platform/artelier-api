package com.artelier.api.integration.wompi.service;

import com.artelier.api.dto.request.PaymentWebhookRequest;

public interface WompiSignatureValidator {
    boolean isValid(PaymentWebhookRequest request);
}
