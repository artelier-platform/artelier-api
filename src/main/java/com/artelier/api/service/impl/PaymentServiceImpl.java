package com.artelier.api.service.impl;

import com.artelier.api.dto.request.PaymentRequest;
import com.artelier.api.dto.response.PaymentResponse;
import com.artelier.api.entity.Order;
import com.artelier.api.entity.Payment;
import com.artelier.api.enums.OrderStatus;
import com.artelier.api.exception.InvalidOrderStateException;
import com.artelier.api.exception.OrderNotFoundException;
import com.artelier.api.exception.PaymentNotFoundException;
import com.artelier.api.integration.wompi.dto.request.*;
import com.artelier.api.integration.wompi.dto.response.WompiFinancialInstitutionsResponse;
import com.artelier.api.integration.wompi.dto.response.WompiTransactionResponse;
import com.artelier.api.integration.wompi.enums.PaymentMethod;
import com.artelier.api.integration.wompi.enums.PaymentStatus;
import com.artelier.api.integration.wompi.service.WompiAcceptanceTokenService;
import com.artelier.api.integration.wompi.service.WompiClient;
import com.artelier.api.integration.wompi.util.WompiIntegritySignatureUtil;
import com.artelier.api.mapper.PaymentMapper;
import com.artelier.api.repository.OrderRepository;
import com.artelier.api.repository.PaymentRepository;
import com.artelier.api.service.OrderService;
import com.artelier.api.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String CURRENCY = "COP";

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PaymentMapper paymentMapper;
    private final WompiClient wompiClient;
    private final WompiAcceptanceTokenService acceptanceTokenService;
    private final WompiIntegritySignatureUtil signatureUtil;

    @Value("${wompi.redirect-url}")
    private String redirectUrl;

    // ─── Create payment ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse createPendingPayment(UUID orderId, PaymentRequest request, String clientIp) {
        return paymentRepository.findActiveByOrderId(orderId)
                .map(paymentMapper::toResponse)
                .orElseGet(() -> createNewPaymentAttempt(orderId, request, clientIp));
    }

    private PaymentResponse createNewPaymentAttempt(UUID orderId, PaymentRequest request, String clientIp) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderStateException(order.getStatus());
        }

        String reference   = "ORDER-" + orderId + "-" + System.currentTimeMillis();
        long amountInCents = toAmountInCents(order.getTotal());

        WompiAcceptanceTokens tokens = acceptanceTokenService.getTokens();
        String signature             = signatureUtil.generate(reference, amountInCents, CURRENCY);

        WompiTransactionRequest wompiRequest = buildWompiRequest(
                tokens, amountInCents, reference, signature,
                order.getUser().getEmail(),
                request,
                clientIp
        );

        log.info("Creating Wompi transaction for order {} method={}",
                orderId, request.getPaymentMethod().getClass().getSimpleName());

        WompiTransactionResponse wompiResponse = wompiClient.createTransaction(wompiRequest);
        WompiTransactionResponse.TransactionData txData = wompiResponse.getData();

        String redirectUrl = extractRedirectUrl(txData);

        orderService.updateOrderStatus(orderId, OrderStatus.PROCESSING);

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotal())
                .status(PaymentStatus.PENDING)
                .paymentMethod(null)
                .reference(reference)
                .wompiTransactionId(txData.getId())
                .redirectUrl(redirectUrl)
                .build();

        return paymentMapper.toResponse(paymentRepository.save(payment));
    }

    private WompiTransactionRequest buildWompiRequest(
            WompiAcceptanceTokens tokens,
            long amountInCents,
            String reference,
            String signature,
            String customerEmail,
            PaymentRequest request,
            String clientIp       // ← nuevo parámetro
    ) {
        PaymentMethodBody paymentMethod = request.getPaymentMethod();

        // Para PSE: inyecta el IP real si el frontend no lo mandó
        if (paymentMethod instanceof PsePaymentMethod pse
                && (pse.getReferenceOne() == null || pse.getReferenceOne().isBlank())) {
            pse.setReferenceOne(clientIp);
        }

        WompiTransactionRequest.WompiTransactionRequestBuilder builder = WompiTransactionRequest.builder()
                .acceptanceToken(tokens.getAcceptanceToken())
                .acceptPersonalAuth(tokens.getAcceptPersonalAuth())
                .amountInCents(amountInCents)
                .currency(CURRENCY)
                .customerEmail(customerEmail)
                .reference(reference)
                .signature(signature)
                .paymentMethod(paymentMethod)
                .redirectUrl(redirectUrl)
                .ip(clientIp);

        if (paymentMethod instanceof PsePaymentMethod && request.getCustomerData() != null) {
            builder.customerData(
                    WompiTransactionRequest.CustomerData.builder()
                            .fullName(request.getCustomerData().getFullName())
                            .phoneNumber(request.getCustomerData().getPhoneNumber())
                            .build()
            );
        }

        return builder.build();
    }

    private String extractRedirectUrl(WompiTransactionResponse.TransactionData txData) {
        if (txData.getPaymentMethod() == null) return null;
        if (txData.getPaymentMethod().getExtra() == null) return null;
        return txData.getPaymentMethod().getExtra().getAsyncPaymentUrl();
    }

    // ─── Confirm payment (webhook) ────────────────────────────────────────────

    @Override
    @Transactional
    public void confirmPayment(PaymentWebhookRequest request) {
        var tx = request.getData().getTransaction();

        Payment payment = paymentRepository.findByReference(tx.getReference())
                .orElseThrow(() -> new PaymentNotFoundException(tx.getReference()));

        if (isFinalStatus(payment.getStatus())) {
            log.info("Webhook ignored — payment {} already {}", payment.getId(), payment.getStatus());
            return;
        }

        Order order = payment.getOrder();
        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new InvalidOrderStateException(order.getStatus());
        }

        payment.setWompiTransactionId(tx.getId());
        resolvePaymentMethod(payment, tx.getPaymentMethodType());

        switch (tx.getStatus()) {
            case "APPROVED" -> {
                payment.setStatus(PaymentStatus.APPROVED);
                payment.setPaidAt(Instant.now());
                orderService.updateOrderStatus(order.getId(), OrderStatus.PAID);
                log.info("Payment {} APPROVED — order {}", payment.getId(), order.getId());
            }
            case "DECLINED" -> {
                payment.setStatus(PaymentStatus.DECLINED);
                orderService.updateOrderStatus(order.getId(), OrderStatus.PENDING_PAYMENT);
                log.info("Payment {} DECLINED — order {}", payment.getId(), order.getId());
            }
            case "VOIDED" -> {
                payment.setStatus(PaymentStatus.VOIDED);
                orderService.updateOrderStatus(order.getId(), OrderStatus.PENDING_PAYMENT);
            }
            default -> {
                payment.setStatus(PaymentStatus.ERROR);
                orderService.updateOrderStatus(order.getId(), OrderStatus.PENDING_PAYMENT);
                log.warn("Payment {} unexpected status={}", payment.getId(), tx.getStatus());
            }
        }

        paymentRepository.save(payment);
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse findByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
    }

    @Override
    public List<WompiFinancialInstitutionsResponse> getFinancialInstitutions() {
        return wompiClient.getFinancialInstitutions().getData();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void resolvePaymentMethod(Payment payment, String paymentMethodType) {
        if (paymentMethodType == null) return;
        try {
            payment.setPaymentMethod(PaymentMethod.valueOf(paymentMethodType));
        } catch (IllegalArgumentException e) {
            log.debug("Payment method {} not in enum — storing null", paymentMethodType);
            payment.setPaymentMethod(null);
        }
    }

    private boolean isFinalStatus(PaymentStatus status) {
        return switch (status) {
            case APPROVED, DECLINED, VOIDED, ERROR -> true;
            case PENDING -> false;
        };
    }

    private long toAmountInCents(BigDecimal amount) {
        return amount.movePointRight(2).longValue();
    }
}