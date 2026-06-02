package com.artelier.api.service;

import com.artelier.api.dto.request.PaymentRequest;
import com.artelier.api.integration.wompi.dto.request.PaymentWebhookRequest;
import com.artelier.api.dto.response.PaymentResponse;
import com.artelier.api.integration.wompi.dto.response.WompiFinancialInstitutionsResponse;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentResponse createPendingPayment(UUID orderId, PaymentRequest request, String clientIp);

    void confirmPayment(PaymentWebhookRequest request);

    PaymentResponse findByOrderId(UUID orderId);

    List<WompiFinancialInstitutionsResponse> getFinancialInstitutions();
}
