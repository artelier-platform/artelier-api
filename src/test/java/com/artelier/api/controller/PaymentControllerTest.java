package com.artelier.api.controller;

import com.artelier.api.config.JacksonTestConfig;
import com.artelier.api.dto.request.PaymentWebhookRequest;
import com.artelier.api.dto.response.PaymentResponse;
import com.artelier.api.entity.enums.PaymentStatus;
import com.artelier.api.exception.OrderNotFoundException;
import com.artelier.api.exception.PaymentNotFoundException;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.PaymentService;
import com.artelier.api.service.WompiSignatureValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonTestConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private WompiSignatureValidator signatureValidator;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    // ─── POST /webhook ────────────────────────────────────────────────────────

    @Test
    void shouldProcessWebhookAndReturn200() throws Exception {
        when(signatureValidator.isValid(any())).thenReturn(true);
        doNothing().when(paymentService).confirmPayment(any());

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildWebhookRequest("transaction.updated"))))
                .andExpect(status().isOk());

        verify(paymentService).confirmPayment(any());
    }

    @Test
    void shouldReturn401WhenSignatureIsInvalid() throws Exception {
        when(signatureValidator.isValid(any())).thenReturn(false);

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildWebhookRequest("transaction.updated"))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentService);
    }

    @Test
    void shouldReturn200AndIgnoreNonTransactionUpdatedEvent() throws Exception {
        when(signatureValidator.isValid(any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildWebhookRequest("transaction.created"))))
                .andExpect(status().isOk());

        verifyNoInteractions(paymentService);
    }

    // ─── POST /orders/{orderId} ───────────────────────────────────────────────

    @Test
    void shouldCreatePendingPaymentAndReturn201() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentResponse response = buildPaymentResponse(orderId, PaymentStatus.PENDING);

        when(paymentService.createPendingPayment(orderId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/orders/" + orderId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturn404WhenOrderNotFoundOnCreate() throws Exception {
        UUID orderId = UUID.randomUUID();

        when(paymentService.createPendingPayment(orderId))
                .thenThrow(new OrderNotFoundException(orderId));

        mockMvc.perform(post("/api/v1/payments/orders/" + orderId))
                .andExpect(status().isNotFound());
    }

    // ─── GET /orders/{orderId} ────────────────────────────────────────────────

    @Test
    void shouldReturnApprovedPaymentStatus() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentResponse response = buildPaymentResponse(orderId, PaymentStatus.APPROVED);

        when(paymentService.findByOrderId(orderId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldReturnPendingPaymentStatus() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentResponse response = buildPaymentResponse(orderId, PaymentStatus.PENDING);

        when(paymentService.findByOrderId(orderId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturn404WhenPaymentNotFound() throws Exception {
        UUID orderId = UUID.randomUUID();

        when(paymentService.findByOrderId(orderId))
                .thenThrow(new PaymentNotFoundException(orderId));

        mockMvc.perform(get("/api/v1/payments/orders/" + orderId))
                .andExpect(status().isNotFound());
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private PaymentWebhookRequest buildWebhookRequest(String event) {
        PaymentWebhookRequest.Transaction tx = new PaymentWebhookRequest.Transaction();
        tx.setId("wompi_tx_001");
        tx.setReference(UUID.randomUUID().toString());
        tx.setStatus("APPROVED");
        tx.setPaymentMethodType("NEQUI");
        tx.setAmountInCents(12000000L);

        PaymentWebhookRequest.SignatureData sig = new PaymentWebhookRequest.SignatureData();
        sig.setChecksum("abc123");
        sig.setProperties(List.of("transaction.id", "transaction.status", "transaction.amount_in_cents"));

        PaymentWebhookRequest.TransactionData data = new PaymentWebhookRequest.TransactionData();
        data.setTransaction(tx);

        PaymentWebhookRequest request = new PaymentWebhookRequest();
        request.setEvent(event);
        request.setData(data);
        request.setSignature(sig);
        request.setTimestamp(1715000000L);
        return request;
    }

    private PaymentResponse buildPaymentResponse(UUID orderId, PaymentStatus status) {
        PaymentResponse response = new PaymentResponse();
        response.setId(UUID.randomUUID());
        response.setOrderId(orderId);
        response.setStatus(status);
        response.setAmount(BigDecimal.valueOf(120000));
        return response;
    }
}