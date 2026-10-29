package com.artelier.api.controller;

import com.artelier.api.config.JacksonTestConfig;
import com.artelier.api.config.SecurityConfig;
import com.artelier.api.dto.request.PaymentRequest;
import com.artelier.api.dto.request.PaymentWebhookRequest;
import com.artelier.api.dto.response.PaymentResponse;
import com.artelier.api.entity.User;
import com.artelier.api.enums.OrderStatus;
import com.artelier.api.enums.Role;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.exception.InvalidOrderStateException;
import com.artelier.api.exception.PaymentNotFoundException;
import com.artelier.api.integration.wompi.dto.request.CardPaymentMethod;
import com.artelier.api.integration.wompi.dto.request.NequiPaymentMethod;
import com.artelier.api.integration.wompi.dto.request.PsePaymentMethod;
import com.artelier.api.integration.wompi.dto.response.WompiFinancialInstitutionsResponse;
import com.artelier.api.integration.wompi.enums.PaymentMethod;
import com.artelier.api.integration.wompi.enums.PaymentStatus;
import com.artelier.api.integration.wompi.exception.WompiException;
import com.artelier.api.integration.wompi.service.WompiSignatureValidator;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.security.UserPrincipal;
import com.artelier.api.service.PaymentService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link PaymentController} with the real {@link SecurityConfig} (filters enabled).
 *
 * <p>Webhook logic tests run with a mock user because SecurityConfig currently has no matcher
 * for {@code POST /payments/webhook}; the anonymous-access test is disabled until it is added.</p>
 */
@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, JacksonTestConfig.class})
@DisplayName("PaymentController")
class PaymentControllerTest {

    private static final Instant PAID_AT = Instant.parse("2026-05-07T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private WompiSignatureValidator signatureValidator;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    private final UserPrincipal buyer = principal(Role.BUYER, "buyer@artelier.com");
    private final UserPrincipal admin = principal(Role.ADMIN, "admin@artelier.com");

    // ------------------------------------------------------------------ fixtures

    private static UserPrincipal principal(Role role, String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setRole(role);
        return new UserPrincipal(user);
    }

    private static PaymentResponse payment(UUID orderId, PaymentStatus status, String redirectUrl) {
        return new PaymentResponse(
                UUID.randomUUID(), orderId, "1292-1602113476-10985",
                "ORDER-550e8400-1715000000000", status, new BigDecimal("120000.00"),
                PaymentMethod.NEQUI, redirectUrl,
                status == PaymentStatus.APPROVED ? PAID_AT : null);
    }

    private static String webhookJson(String event) {
        return """
                {
                  "event": "%s",
                  "data": {
                    "transaction": {
                      "id": "1292-1602113476-10985",
                      "reference": "ORDER-550e8400-1715000000000",
                      "status": "APPROVED",
                      "payment_method_type": "NEQUI",
                      "amount_in_cents": 12000000
                    }
                  },
                  "signature": {
                    "checksum": "abc123",
                    "properties": ["transaction.id", "transaction.status", "transaction.amount_in_cents"]
                  },
                  "timestamp": 1715000000
                }
                """.formatted(event);
    }

    private static final String CARD_BODY = """
            {"payment_method": {"type": "CARD", "token": "tok_test_123", "installments": 3}}
            """;

    private static final String NEQUI_BODY = """
            {"payment_method": {"type": "NEQUI", "phone_number": "3107654321"}}
            """;

    private static final String PSE_BODY = """
            {
              "payment_method": {
                "type": "PSE",
                "user_type": 0,
                "user_legal_id_type": "CC",
                "user_legal_id": "1099888777",
                "financial_institution_code": "1007",
                "payment_description": "Payment for Artelier order"
              },
              "customer_data": {"full_name": "María García", "phone_number": "+573001112233"}
            }
            """;

    record InvalidBody(String label, String json, String field) {
        @Override
        public String toString() {
            return label;
        }
    }

    static Stream<InvalidBody> invalidPaymentBodies() {
        return Stream.of(
                new InvalidBody("payment_method missing",
                        "{}", "paymentMethod"),
                new InvalidBody("card token blank",
                        "{\"payment_method\": {\"type\": \"CARD\", \"token\": \"  \"}}",
                        "paymentMethod.token"),
                new InvalidBody("card installments below 1",
                        "{\"payment_method\": {\"type\": \"CARD\", \"token\": \"tok\", \"installments\": 0}}",
                        "paymentMethod.installments"),
                new InvalidBody("nequi phone blank",
                        "{\"payment_method\": {\"type\": \"NEQUI\", \"phone_number\": \"\"}}",
                        "paymentMethod.phoneNumber"),
                new InvalidBody("pse fields missing",
                        "{\"payment_method\": {\"type\": \"PSE\"}}",
                        "paymentMethod.userLegalId"),
                new InvalidBody("pse description over 64 chars",
                        "{\"payment_method\": {\"type\": \"PSE\", \"user_type\": 0, \"user_legal_id_type\": \"CC\","
                                + " \"user_legal_id\": \"1\", \"financial_institution_code\": \"1\","
                                + " \"payment_description\": \"" + "x".repeat(65) + "\"}}",
                        "paymentMethod.paymentDescription")
        );
    }

    // ------------------------------------------------------------------ POST /payments/webhook

    @Nested
    @DisplayName("POST /payments/webhook")
    @WithMockUser(roles = "BUYER")
    class Webhook {

        @Test
        @DisplayName("401 – invalid signature is rejected and the payment is not confirmed")
        void invalidSignature() throws Exception {
            when(signatureValidator.isValid(any())).thenReturn(false);

            mockMvc.perform(post("/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(webhookJson("transaction.updated")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string(""));

            verifyNoInteractions(paymentService);
        }

        @Test
        @DisplayName("200 – valid transaction.updated confirms the payment with the parsed payload")
        void transactionUpdated() throws Exception {
            when(signatureValidator.isValid(any())).thenReturn(true);

            mockMvc.perform(post("/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(webhookJson("transaction.updated")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));

            ArgumentCaptor<PaymentWebhookRequest> captor = ArgumentCaptor.forClass(PaymentWebhookRequest.class);
            verify(paymentService).confirmPayment(captor.capture());
            PaymentWebhookRequest received = captor.getValue();
            assertThat(received.event()).isEqualTo("transaction.updated");
            assertThat(received.timestamp()).isEqualTo(1715000000L);
            assertThat(received.signature().checksum()).isEqualTo("abc123");
            assertThat(received.signature().properties())
                    .containsExactly("transaction.id", "transaction.status", "transaction.amount_in_cents");
            assertThat(received.data().transaction().id()).isEqualTo("1292-1602113476-10985");
            assertThat(received.data().transaction().reference()).isEqualTo("ORDER-550e8400-1715000000000");
            assertThat(received.data().transaction().status()).isEqualTo("APPROVED");
            assertThat(received.data().transaction().paymentMethodType()).isEqualTo("NEQUI");
            assertThat(received.data().transaction().amountInCents()).isEqualTo(12000000L);
        }

        @Test
        @DisplayName("200 – other events are acknowledged and ignored")
        void otherEventIgnored() throws Exception {
            when(signatureValidator.isValid(any())).thenReturn(true);

            mockMvc.perform(post("/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(webhookJson("nequi_token.updated")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));

            verify(paymentService, never()).confirmPayment(any());
        }

        @Test
        @DisplayName("400 – malformed JSON never reaches the validator or the service")
        void malformedJson() throws Exception {
            mockMvc.perform(post("/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ not-json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid request body. Please check JSON format."));

            verifyNoInteractions(signatureValidator, paymentService);
        }

        @Test
        @DisplayName("404 – payment reference unknown to the service")
        void paymentNotFound() throws Exception {
            when(signatureValidator.isValid(any())).thenReturn(true);
            doThrow(new PaymentNotFoundException("ORDER-550e8400-1715000000000"))
                    .when(paymentService).confirmPayment(any());

            mockMvc.perform(post("/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(webhookJson("transaction.updated")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message")
                            .value("Payment not found for reference: ORDER-550e8400-1715000000000"));
        }

        @Test
        @DisplayName("500 – unexpected failure is mapped to the generic error body")
        void unexpectedFailure() throws Exception {
            when(signatureValidator.isValid(any())).thenReturn(true);
            doThrow(new IllegalStateException("db down")).when(paymentService).confirmPayment(any());

            mockMvc.perform(post("/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(webhookJson("transaction.updated")))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Something went wrong"));
        }
    }

    @Nested
    @DisplayName("POST /payments/webhook – anonymous access")
    class WebhookAnonymous {

        /**
         * The controller documents the webhook as public (Wompi cannot send a JWT), but
         * SecurityConfig has no matcher for it, so {@code anyRequest().authenticated()} answers
         * 401 to Wompi. Enable this test after adding
         * {@code .requestMatchers(HttpMethod.POST, "/payments/webhook").permitAll()}.
         */
        @Test
        @Disabled("SecurityConfig does not permit POST /payments/webhook yet")
        @DisplayName("200 – anonymous caller (Wompi) reaches the webhook")
        void anonymousCanCallWebhook() throws Exception {
            when(signatureValidator.isValid(any())).thenReturn(true);

            mockMvc.perform(post("/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(webhookJson("transaction.updated")))
                    .andExpect(status().isOk());

            verify(paymentService).confirmPayment(any());
        }
    }

    // ------------------------------------------------------------------ POST /payments/orders/{orderId}

    @Nested
    @DisplayName("POST /payments/orders/{orderId}")
    class CreatePayment {

        @Test
        @DisplayName("201 – CARD: maps the response and parses the polymorphic payment method")
        void cardSuccess() throws Exception {
            UUID orderId = UUID.randomUUID();
            PaymentResponse response = payment(orderId, PaymentStatus.PENDING, null);
            when(paymentService.createPendingPayment(eq(orderId), any(), any(), any())).thenReturn(response);

            mockMvc.perform(post("/payments/orders/{orderId}", orderId)
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CARD_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Payment created successfully"))
                    .andExpect(jsonPath("$.data.id").value(response.id().toString()))
                    .andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
                    .andExpect(jsonPath("$.data.reference").value("ORDER-550e8400-1715000000000"))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.amount").value(120000.00))
                    .andExpect(jsonPath("$.data.redirectUrl").doesNotExist())
                    .andExpect(jsonPath("$.data.paidAt").doesNotExist());

            ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
            verify(paymentService).createPendingPayment(eq(orderId), captor.capture(), any(), same(buyer.getUser()));
            assertThat(captor.getValue().paymentMethod()).isInstanceOf(CardPaymentMethod.class);
            CardPaymentMethod card = (CardPaymentMethod) captor.getValue().paymentMethod();
            assertThat(card.getToken()).isEqualTo("tok_test_123");
            assertThat(card.getInstallments()).isEqualTo(3);
            assertThat(captor.getValue().customerData()).isNull();
        }

        @Test
        @DisplayName("201 – NEQUI: parses phone_number")
        void nequiSuccess() throws Exception {
            UUID orderId = UUID.randomUUID();
            when(paymentService.createPendingPayment(eq(orderId), any(), any(), any()))
                    .thenReturn(payment(orderId, PaymentStatus.PENDING, null));

            mockMvc.perform(post("/payments/orders/{orderId}", orderId)
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(NEQUI_BODY))
                    .andExpect(status().isCreated());

            ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
            verify(paymentService).createPendingPayment(eq(orderId), captor.capture(), any(), any());
            assertThat(captor.getValue().paymentMethod()).isInstanceOf(NequiPaymentMethod.class);
            assertThat(((NequiPaymentMethod) captor.getValue().paymentMethod()).getPhoneNumber())
                    .isEqualTo("3107654321");
        }

        @Test
        @DisplayName("201 – PSE: returns redirectUrl and parses method and customer data")
        void pseSuccess() throws Exception {
            UUID orderId = UUID.randomUUID();
            when(paymentService.createPendingPayment(eq(orderId), any(), any(), any()))
                    .thenReturn(payment(orderId, PaymentStatus.PENDING, "https://bank.example.com/pse/redirect"));

            mockMvc.perform(post("/payments/orders/{orderId}", orderId)
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PSE_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.redirectUrl").value("https://bank.example.com/pse/redirect"));

            ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
            verify(paymentService).createPendingPayment(eq(orderId), captor.capture(), any(), any());
            assertThat(captor.getValue().paymentMethod()).isInstanceOf(PsePaymentMethod.class);
            PsePaymentMethod pse = (PsePaymentMethod) captor.getValue().paymentMethod();
            assertThat(pse.getUserType()).isZero();
            assertThat(pse.getUserLegalIdType()).isEqualTo("CC");
            assertThat(pse.getUserLegalId()).isEqualTo("1099888777");
            assertThat(pse.getFinancialInstitutionCode()).isEqualTo("1007");
            assertThat(captor.getValue().customerData().fullName()).isEqualTo("María García");
            assertThat(captor.getValue().customerData().phoneNumber()).isEqualTo("+573001112233");
        }

        @Test
        @DisplayName("201 – client IP is the first X-Forwarded-For address")
        void clientIpFromForwardedFor() throws Exception {
            UUID orderId = UUID.randomUUID();
            when(paymentService.createPendingPayment(any(), any(), any(), any()))
                    .thenReturn(payment(orderId, PaymentStatus.PENDING, null));

            mockMvc.perform(post("/payments/orders/{orderId}", orderId)
                            .with(user(buyer))
                            .header("X-Forwarded-For", "203.0.113.5, 10.0.0.1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CARD_BODY))
                    .andExpect(status().isCreated());

            verify(paymentService).createPendingPayment(eq(orderId), any(), eq("203.0.113.5"), any());
        }

        @Test
        @DisplayName("201 – blank X-Forwarded-For falls back to the remote address")
        void clientIpFallsBackToRemoteAddr() throws Exception {
            UUID orderId = UUID.randomUUID();
            when(paymentService.createPendingPayment(any(), any(), any(), any()))
                    .thenReturn(payment(orderId, PaymentStatus.PENDING, null));

            mockMvc.perform(post("/payments/orders/{orderId}", orderId)
                            .with(user(buyer))
                            .header("X-Forwarded-For", "   ")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CARD_BODY))
                    .andExpect(status().isCreated());

            verify(paymentService).createPendingPayment(eq(orderId), any(), eq("127.0.0.1"), any());
        }

        @Test
        @DisplayName("201 – without X-Forwarded-For uses the remote address")
        void clientIpWithoutHeader() throws Exception {
            UUID orderId = UUID.randomUUID();
            when(paymentService.createPendingPayment(any(), any(), any(), any()))
                    .thenReturn(payment(orderId, PaymentStatus.PENDING, null));

            mockMvc.perform(post("/payments/orders/{orderId}", orderId)
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CARD_BODY))
                    .andExpect(status().isCreated());

            verify(paymentService).createPendingPayment(eq(orderId), any(), eq("127.0.0.1"), any());
        }

        @ParameterizedTest(name = "400 – {0}")
        @MethodSource("com.artelier.api.controller.PaymentControllerTest#invalidPaymentBodies")
        void validationFailures(InvalidBody invalid) throws Exception {
            mockMvc.perform(post("/payments/orders/{orderId}", UUID.randomUUID())
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalid.json()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.data[?(@.field == '" + invalid.field() + "')]").isNotEmpty());

            verifyNoInteractions(paymentService);
        }

        @Test
        @DisplayName("400 – unknown payment method type")
        void unknownMethodType() throws Exception {
            mockMvc.perform(post("/payments/orders/{orderId}", UUID.randomUUID())
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"payment_method\": {\"type\": \"BITCOIN\"}}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(paymentService);
        }

        @Test
        @DisplayName("400 – malformed JSON")
        void malformedJson() throws Exception {
            mockMvc.perform(post("/payments/orders/{orderId}", UUID.randomUUID())
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ not-json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid request body. Please check JSON format."));

            verifyNoInteractions(paymentService);
        }

        @Test
        @DisplayName("400 – missing request body")
        void missingBody() throws Exception {
            mockMvc.perform(post("/payments/orders/{orderId}", UUID.randomUUID())
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(paymentService);
        }

        @Test
        @DisplayName("400 – orderId that is not a UUID")
        void orderIdNotAUuid() throws Exception {
            mockMvc.perform(post("/payments/orders/{orderId}", "not-a-uuid")
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CARD_BODY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid value for parameter 'orderId'"));

            verifyNoInteractions(paymentService);
        }

        @Test
        @DisplayName("400 – Wompi error is wrapped with provider and code")
        void wompiError() throws Exception {
            when(paymentService.createPendingPayment(any(), any(), any(), any()))
                    .thenThrow(new WompiException("INPUT_VALIDATION_ERROR", "Invalid card token",
                            HttpStatus.UNPROCESSABLE_CONTENT));

            mockMvc.perform(post("/payments/orders/{orderId}", UUID.randomUUID())
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CARD_BODY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid card token"))
                    .andExpect(jsonPath("$.data.provider").value("WOMPI"))
                    .andExpect(jsonPath("$.data.code").value("INPUT_VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("403 – order belongs to another user")
        void foreignOrder() throws Exception {
            UUID orderId = UUID.randomUUID();
            when(paymentService.createPendingPayment(eq(orderId), any(), any(), any()))
                    .thenThrow(ArtelierException.forbidden("Access denied to this order"));

            mockMvc.perform(post("/payments/orders/{orderId}", orderId)
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CARD_BODY))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Access denied to this order"));

            verify(paymentService).createPendingPayment(eq(orderId), any(), any(), same(buyer.getUser()));
        }

        @Test
        @DisplayName("404 – order does not exist")
        void orderNotFound() throws Exception {
            when(paymentService.createPendingPayment(any(), any(), any(), any()))
                    .thenThrow(ArtelierException.notFound("Order not found"));

            mockMvc.perform(post("/payments/orders/{orderId}", UUID.randomUUID())
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CARD_BODY))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Order not found"));
        }

        @Test
        @DisplayName("409 – order is not in a payable state")
        void invalidOrderState() throws Exception {
            when(paymentService.createPendingPayment(any(), any(), any(), any()))
                    .thenThrow(new InvalidOrderStateException(OrderStatus.PAID));

            mockMvc.perform(post("/payments/orders/{orderId}", UUID.randomUUID())
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CARD_BODY))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message")
                            .value("Cannot process payment for order with status: PAID"));
        }

        @Test
        @DisplayName("500 – unexpected failure is mapped to the generic error body")
        void unexpectedFailure() throws Exception {
            when(paymentService.createPendingPayment(any(), any(), any(), any()))
                    .thenThrow(new IllegalStateException("db down"));

            mockMvc.perform(post("/payments/orders/{orderId}", UUID.randomUUID())
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CARD_BODY))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Something went wrong"));
        }
    }

    @Nested
    @DisplayName("POST /payments/orders/{orderId} – anonymous")
    class CreatePaymentAnonymous {

        @Test
        @DisplayName("401 – anonymous request never reaches the service")
        void anonymousRejected() throws Exception {
            mockMvc.perform(post("/payments/orders/{orderId}", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CARD_BODY))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(paymentService);
        }
    }

    // ------------------------------------------------------------------ GET /payments/orders/{orderId}

    @Nested
    @DisplayName("GET /payments/orders/{orderId}")
    class GetPaymentByOrder {

        @Test
        @DisplayName("200 – returns the approved payment with every field")
        void approved() throws Exception {
            UUID orderId = UUID.randomUUID();
            PaymentResponse response = payment(orderId, PaymentStatus.APPROVED, null);
            when(paymentService.findByOrderId(eq(orderId), any())).thenReturn(response);

            mockMvc.perform(get("/payments/orders/{orderId}", orderId)
                    .with(user(buyer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Payment retrieved successfully"))
                    .andExpect(jsonPath("$.data.id").value(response.id().toString()))
                    .andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
                    .andExpect(jsonPath("$.data.wompiTransactionId").value("1292-1602113476-10985"))
                    .andExpect(jsonPath("$.data.reference").value("ORDER-550e8400-1715000000000"))
                    .andExpect(jsonPath("$.data.status").value("APPROVED"))
                    .andExpect(jsonPath("$.data.amount").value(120000.00))
                    .andExpect(jsonPath("$.data.paymentMethod").value("NEQUI"))
                    .andExpect(jsonPath("$.data.paidAt").value("2026-05-07T00:00:00Z"));

            verify(paymentService).findByOrderId(eq(orderId), same(buyer.getUser()));
        }

        @Test
        @DisplayName("200 – pending payment has no paidAt")
        void pending() throws Exception {
            UUID orderId = UUID.randomUUID();
            when(paymentService.findByOrderId(eq(orderId), any())).thenReturn(payment(orderId, PaymentStatus.PENDING, null));

            mockMvc.perform(get("/payments/orders/{orderId}", orderId)
                    .with(user(buyer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.paidAt").doesNotExist());
        }

        @Test
        @DisplayName("200 – ADMIN can also read a payment")
        void adminAllowed() throws Exception {
            UUID orderId = UUID.randomUUID();
            when(paymentService.findByOrderId(eq(orderId), any())).thenReturn(payment(orderId, PaymentStatus.DECLINED, null));

            mockMvc.perform(get("/payments/orders/{orderId}", orderId)
                    .with(user(admin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("DECLINED"));

            verify(paymentService).findByOrderId(eq(orderId), same(admin.getUser()));
        }

        @Test
        @DisplayName("400 – orderId that is not a UUID")
        void orderIdNotAUuid() throws Exception {
            mockMvc.perform(get("/payments/orders/{orderId}", "not-a-uuid")
                    .with(user(buyer)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid value for parameter 'orderId'"));

            verifyNoInteractions(paymentService);
        }

        @Test
        @DisplayName("403 – order belongs to another user")
        void foreignOrder() throws Exception {
            UUID orderId = UUID.randomUUID();
            when(paymentService.findByOrderId(eq(orderId), any()))
                    .thenThrow(ArtelierException.forbidden("Access denied to this order"));

            mockMvc.perform(get("/payments/orders/{orderId}", orderId)
                    .with(user(buyer)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Access denied to this order"));

            verify(paymentService).findByOrderId(eq(orderId), same(buyer.getUser()));
        }

        @Test
        @DisplayName("404 – order or payment not found")
        void notFound() throws Exception {
            UUID orderId = UUID.randomUUID();
            when(paymentService.findByOrderId(eq(orderId), any())).thenThrow(new PaymentNotFoundException(orderId));

            mockMvc.perform(get("/payments/orders/{orderId}", orderId)
                    .with(user(buyer)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("No payment found for order: " + orderId));
        }

        @Test
        @DisplayName("401 – anonymous request never reaches the service")
        void anonymousRejected() throws Exception {
            mockMvc.perform(get("/payments/orders/{orderId}", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(paymentService);
        }
    }

    // ------------------------------------------------------------------ GET /payments/financial-institutions

    @Nested
    @DisplayName("GET /payments/financial-institutions")
    class FinancialInstitutions {

        private WompiFinancialInstitutionsResponse institution(String code, String name) {
            WompiFinancialInstitutionsResponse response = new WompiFinancialInstitutionsResponse();
            response.setCode(code);
            response.setName(name);
            return response;
        }

        @Test
        @DisplayName("200 – returns institutions with Wompi's snake_case field names")
        @WithMockUser(roles = "BUYER")
        void success() throws Exception {
            when(paymentService.getFinancialInstitutions()).thenReturn(List.of(
                    institution("1007", "Bancolombia"),
                    institution("1022", "Banco de Bogotá")));

            mockMvc.perform(get("/payments/financial-institutions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Financial institutions retrieved successfully"))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].financial_institution_code").value("1007"))
                    .andExpect(jsonPath("$.data[0].financial_institution_name").value("Bancolombia"))
                    .andExpect(jsonPath("$.data[1].financial_institution_code").value("1022"))
                    .andExpect(jsonPath("$.data[1].financial_institution_name").value("Banco de Bogotá"));

            verify(paymentService).getFinancialInstitutions();
        }

        @Test
        @DisplayName("200 – no institutions returns an empty list")
        @WithMockUser(roles = "BUYER")
        void empty() throws Exception {
            when(paymentService.getFinancialInstitutions()).thenReturn(List.of());

            mockMvc.perform(get("/payments/financial-institutions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("400 – Wompi failure is wrapped with provider and code")
        @WithMockUser(roles = "BUYER")
        void wompiFailure() throws Exception {
            when(paymentService.getFinancialInstitutions())
                    .thenThrow(new WompiException("INVALID_KEY",
                            "Wompi authentication failed. Check your API keys.",
                            HttpStatus.INTERNAL_SERVER_ERROR));

            mockMvc.perform(get("/payments/financial-institutions"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data.provider").value("WOMPI"))
                    .andExpect(jsonPath("$.data.code").value("INVALID_KEY"));
        }

        @Test
        @DisplayName("500 – unexpected failure is mapped to the generic error body")
        @WithMockUser(roles = "BUYER")
        void unexpectedFailure() throws Exception {
            when(paymentService.getFinancialInstitutions()).thenThrow(new IllegalStateException("boom"));

            mockMvc.perform(get("/payments/financial-institutions"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Something went wrong"));
        }

        @Test
        @DisplayName("401 – anonymous request never reaches the service")
        void anonymousRejected() throws Exception {
            mockMvc.perform(get("/payments/financial-institutions"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(paymentService);
        }
    }
}
