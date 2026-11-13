package com.artelier.api.integration.wompi.service;

import com.artelier.api.payment.dto.PaymentWebhookRequest;

public interface WompiSignatureValidator {
    boolean isValid(PaymentWebhookRequest request);
}
