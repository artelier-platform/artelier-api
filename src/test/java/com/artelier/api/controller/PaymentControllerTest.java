package com.artelier.api.controller;

import com.artelier.api.config.JacksonTestConfig;
import com.artelier.api.dto.request.PaymentRequest;
import com.artelier.api.dto.request.PaymentWebhookRequest;
import com.artelier.api.dto.response.PaymentResponse;
import com.artelier.api.integration.wompi.dto.response.WompiFinancialInstitutionsResponse;
import com.artelier.api.integration.wompi.enums.PaymentStatus;
import com.artelier.api.exception.OrderNotFoundException;
import com.artelier.api.exception.PaymentNotFoundException;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.PaymentService;
import com.artelier.api.integration.wompi.service.WompiSignatureValidator;
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

import static org.mockito.ArgumentMatchers.*;
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

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void shouldProcessWebhookAndReturn200() throws Exception {
        when(signatureValidator.isValid(any())).thenReturn(true);
        doNothing().when(paymentService).confirmPayment(any());

        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildWebhookRequest("transaction.updated"))))
                .andExpect(status().isOk());

        verify(paymentService).confirmPayment(any());
    }

    @Test
    void shouldReturn401WhenSignatureIsInvalid() throws Exception {
        when(signatureValidator.isValid(any())).thenReturn(false);

        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildWebhookRequest("transaction.updated"))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentService);
    }

    @Test
    void shouldReturn200AndIgnoreNonTransactionUpdatedEvent() throws Exception {
        when(signatureValidator.isValid(any())).thenReturn(true);

        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildWebhookRequest("transaction.created"))))
                .andExpect(status().isOk());

        verifyNoInteractions(paymentService);
    }


    @Test
    void shouldCreatePendingPaymentAndReturn201() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentResponse response = buildPaymentResponse(orderId, PaymentStatus.PENDING);

        when(paymentService.createPendingPayment(
                eq(orderId), any(PaymentRequest.class), anyString()))
                .thenReturn(response);

        mockMvc.perform(post("/payments/orders/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCardPaymentRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void shouldReturn404WhenOrderNotFoundOnCreate() throws Exception {
        UUID orderId = UUID.randomUUID();

        when(paymentService.createPendingPayment(
                eq(orderId), any(PaymentRequest.class), anyString()))
                .thenThrow(new OrderNotFoundException(orderId));

        mockMvc.perform(post("/payments/orders/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCardPaymentRequestJson()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenPaymentRequestBodyIsMissing() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(post("/payments/orders/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void shouldCreatePendingPaymentWithNequiAndReturn201() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentResponse response = buildPaymentResponse(orderId, PaymentStatus.PENDING);

        when(paymentService.createPendingPayment(
                eq(orderId), any(PaymentRequest.class), anyString()))
                .thenReturn(response);

        mockMvc.perform(post("/payments/orders/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "payment_method": {
                            "type": "NEQUI",
                            "phone_number": "3107654321"
                          }
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void shouldCreatePendingPaymentWithPseAndReturn201() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentResponse response = buildPaymentResponse(orderId, PaymentStatus.PENDING);

        when(paymentService.createPendingPayment(
                eq(orderId), any(PaymentRequest.class), anyString()))
                .thenReturn(response);

        mockMvc.perform(post("/payments/orders/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "payment_method": {
                            "type": "PSE",
                            "user_type": 0,
                            "user_legal_id_type": "CC",
                            "user_legal_id": "1099888777",
                            "financial_institution_code": "1007",
                            "payment_description": "Payment for Artelier order"
                          },
                          "customer_data": {
                            "full_name": "María García",
                            "phone_number": "+573001112233"
                          }
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }


    @Test
    void shouldReturnApprovedPaymentStatus() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentResponse response = buildPaymentResponse(orderId, PaymentStatus.APPROVED);

        when(paymentService.findByOrderId(orderId)).thenReturn(response);

        mockMvc.perform(get("/payments/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void shouldReturnPendingPaymentStatus() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentResponse response = buildPaymentResponse(orderId, PaymentStatus.PENDING);

        when(paymentService.findByOrderId(orderId)).thenReturn(response);

        mockMvc.perform(get("/payments/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void shouldReturn404WhenPaymentNotFound() throws Exception {
        UUID orderId = UUID.randomUUID();

        when(paymentService.findByOrderId(orderId))
                .thenThrow(new PaymentNotFoundException(orderId));

        mockMvc.perform(get("/payments/orders/" + orderId))
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldReturnFinancialInstitutions() throws Exception {
        WompiFinancialInstitutionsResponse bank = new WompiFinancialInstitutionsResponse();
        bank.setCode("1007");
        bank.setName("BANCOLOMBIA");

        when(paymentService.getFinancialInstitutions()).thenReturn(List.of(bank));

        mockMvc.perform(get("/payments/financial-institutions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].financial_institution_code").value("1007"))
                .andExpect(jsonPath("$.data[0].financial_institution_name").value("BANCOLOMBIA"));
    }

    @Test
    void shouldReturnEmptyListWhenNoInstitutions() throws Exception {
        when(paymentService.getFinancialInstitutions()).thenReturn(List.of());

        mockMvc.perform(get("/payments/financial-institutions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void shouldUseForwardedIpWhenHeaderExists() throws Exception {
        UUID orderId = UUID.randomUUID();

        PaymentResponse response =
                buildPaymentResponse(orderId, PaymentStatus.PENDING);

        when(paymentService.createPendingPayment(
                eq(orderId),
                any(PaymentRequest.class),
                anyString()))
                .thenReturn(response);

        mockMvc.perform(post("/payments/orders/" + orderId)
                        .header("X-Forwarded-For", "203.0.113.1, 10.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCardPaymentRequestJson()))
                .andExpect(status().isCreated());

        verify(paymentService)
                .createPendingPayment(
                        eq(orderId),
                        any(PaymentRequest.class),
                        eq("203.0.113.1")
                );
    }

    @Test
    void shouldUseRemoteAddressWhenForwardedHeaderMissing() throws Exception {
        UUID orderId = UUID.randomUUID();

        PaymentResponse response =
                buildPaymentResponse(orderId, PaymentStatus.PENDING);

        when(paymentService.createPendingPayment(
                eq(orderId),
                any(PaymentRequest.class),
                anyString()))
                .thenReturn(response);

        mockMvc.perform(post("/payments/orders/" + orderId)
                        .with(req -> {
                            req.setRemoteAddr("127.0.0.1");
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCardPaymentRequestJson()))
                .andExpect(status().isCreated());

        verify(paymentService)
                .createPendingPayment(
                        eq(orderId),
                        any(PaymentRequest.class),
                        eq("127.0.0.1")
                );
    }

    private String buildCardPaymentRequestJson() {
        return """
        {
          "payment_method": {
            "type": "CARD",
            "token": "tok_test_123456",
            "installments": 1
          }
        }
        """;
    }

    private PaymentWebhookRequest buildWebhookRequest(String event) {
        PaymentWebhookRequest.Transaction tx = new PaymentWebhookRequest.Transaction();
        tx.setId("wompi_tx_001");
        tx.setReference(UUID.randomUUID().toString());
        tx.setStatus("APPROVED");
        tx.setPaymentMethodType("NEQUI");
        tx.setAmountInCents(12000000L);

        PaymentWebhookRequest.SignatureData sig = new PaymentWebhookRequest.SignatureData();
        sig.setChecksum("abc123");
        sig.setProperties(List.of(
                "transaction.id",
                "transaction.status",
                "transaction.amount_in_cents"
        ));

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