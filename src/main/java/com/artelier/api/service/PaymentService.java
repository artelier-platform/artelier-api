package com.artelier.api.service;

import com.artelier.api.dto.request.PaymentWebhookRequest;
import com.artelier.api.dto.response.PaymentResponse;
import java.util.UUID;

public interface PaymentService {
    void confirmPayment(PaymentWebhookRequest request);
    PaymentResponse createPendingPayment(UUID orderId);
    PaymentResponse findByOrderId(UUID orderId);
}
