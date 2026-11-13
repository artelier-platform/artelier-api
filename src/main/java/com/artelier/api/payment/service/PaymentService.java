package com.artelier.api.payment.service;

import com.artelier.api.payment.dto.PaymentRequest;
import com.artelier.api.payment.dto.PaymentWebhookRequest;
import com.artelier.api.payment.dto.PaymentResponse;
import com.artelier.api.user.entity.User;
import com.artelier.api.integration.wompi.dto.response.WompiFinancialInstitutionsResponse;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentResponse createPendingPayment(UUID orderId, PaymentRequest request, String clientIp, User requester);

    void confirmPayment(PaymentWebhookRequest request);

    PaymentResponse findByOrderId(UUID orderId, User requester);

    List<WompiFinancialInstitutionsResponse> getFinancialInstitutions();
}
