package com.artelier.api.service;

import com.artelier.api.dto.request.PaymentRequest;
import com.artelier.api.dto.request.PaymentWebhookRequest;
import com.artelier.api.dto.response.PaymentResponse;
import com.artelier.api.entity.Order;
import com.artelier.api.entity.Payment;
import com.artelier.api.entity.User;
import com.artelier.api.enums.OrderStatus;
import com.artelier.api.enums.Role;
import com.artelier.api.exception.InvalidOrderStateException;
import com.artelier.api.exception.OrderNotFoundException;
import com.artelier.api.exception.PaymentNotFoundException;
import com.artelier.api.integration.wompi.dto.request.CardPaymentMethod;
import com.artelier.api.integration.wompi.dto.request.PsePaymentMethod;
import com.artelier.api.integration.wompi.dto.request.WompiAcceptanceTokens;
import com.artelier.api.integration.wompi.dto.request.WompiTransactionRequest;
import com.artelier.api.integration.wompi.dto.response.WompiFinancialInstitutionsListResponse;
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
import com.artelier.api.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Business logic of {@link PaymentServiceImpl} beyond order ownership (covered by
 * {@link PaymentServiceTest}): creation of a new Wompi payment attempt, webhook confirmation
 * and the financial-institutions lookup.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl – payment creation, webhook confirmation and institutions")
class PaymentServiceImplTest {

    private static final String CLIENT_IP = "203.0.113.5";
    private static final String REDIRECT_URL = "https://artelier.test/payment/result";
    private static final String REFERENCE = "ORDER-fixed-reference-1";
    private static final String WOMPI_TX_ID = "wompi-tx-1";
    private static final BigDecimal TOTAL = new BigDecimal("120000.00");
    private static final long TOTAL_IN_CENTS = 12_000_000L;

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderService orderService;
    @Mock private PaymentMapper paymentMapper;
    @Mock private WompiClient wompiClient;
    @Mock private WompiAcceptanceTokenService acceptanceTokenService;
    @Mock private WompiIntegritySignatureUtil signatureUtil;

    @InjectMocks
    private PaymentServiceImpl service;

    private final UUID orderId = UUID.randomUUID();
    private final User owner = user();

    @BeforeEach
    void setUp() {
        // @Value field: not populated by @InjectMocks.
        ReflectionTestUtils.setField(service, "redirectUrl", REDIRECT_URL);
    }

    // ------------------------------------------------------------------ fixtures

    private static User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("buyer@artelier.com");
        user.setRole(Role.BUYER);
        return user;
    }

    private Order order(OrderStatus status, BigDecimal total) {
        return Order.builder()
                .id(orderId)
                .user(owner)
                .status(status)
                .subtotal(total)
                .total(total)
                .build();
    }

    private static PaymentRequest cardRequest() {
        return new PaymentRequest(
                CardPaymentMethod.builder().token("tok_test_123").installments(1).build(), null);
    }

    private static PsePaymentMethod.PsePaymentMethodBuilder pseBuilder() {
        return PsePaymentMethod.builder()
                .userType(0)
                .userLegalIdType("CC")
                .userLegalId("1099888777")
                .financialInstitutionCode("1")
                .paymentDescription("Artelier order");
    }

    private PaymentResponse paymentResponse() {
        return new PaymentResponse(
                UUID.randomUUID(), orderId, WOMPI_TX_ID, REFERENCE, PaymentStatus.PENDING,
                TOTAL, null, null, null);
    }

    private static WompiTransactionResponse.PaymentMethodData methodWithRedirect(String url) {
        WompiTransactionResponse.ExtraData extra = new WompiTransactionResponse.ExtraData();
        extra.setAsyncPaymentUrl(url);
        WompiTransactionResponse.PaymentMethodData method = new WompiTransactionResponse.PaymentMethodData();
        method.setType("PSE");
        method.setExtra(extra);
        return method;
    }

    private static WompiTransactionResponse wompiResponse(WompiTransactionResponse.PaymentMethodData method) {
        WompiTransactionResponse.TransactionData data = new WompiTransactionResponse.TransactionData();
        data.setId(WOMPI_TX_ID);
        data.setPaymentMethod(method);
        WompiTransactionResponse response = new WompiTransactionResponse();
        response.setData(data);
        return response;
    }

    private Payment pendingPayment(Order order) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .order(order)
                .reference(REFERENCE)
                .status(PaymentStatus.PENDING)
                .amount(TOTAL)
                .build();
    }

    private static PaymentWebhookRequest webhook(String status, String paymentMethodType) {
        return new PaymentWebhookRequest(
                "transaction.updated",
                new PaymentWebhookRequest.TransactionData(
                        new PaymentWebhookRequest.Transaction(
                                WOMPI_TX_ID, REFERENCE, status, paymentMethodType, TOTAL_IN_CENTS)),
                new PaymentWebhookRequest.SignatureData("checksum", List.of()),
                1_718_730_000L);
    }

    // ------------------------------------------------------------------ createPendingPayment

    @Nested
    @DisplayName("createPendingPayment – new payment attempt")
    class CreateNewAttempt {

        /**
         * Stubs the whole happy path; each test overrides only what it needs.
         */
        private void stubNewAttempt(Order order, WompiTransactionResponse wompiResponse) {
            when(paymentRepository.findActiveByOrderId(orderId))
                    .thenReturn(Optional.empty());
            when(orderRepository.findById(orderId))
                    .thenReturn(Optional.of(order));
            when(acceptanceTokenService.getTokens())
                    .thenReturn(new WompiAcceptanceTokens(
                            "acceptance-token", "personal-auth-token"));
            when(signatureUtil.generate(anyString(), anyLong(), anyString()))
                    .thenReturn("integrity-signature");
            when(wompiClient.createTransaction(any(WompiTransactionRequest.class)))
                    .thenReturn(wompiResponse);
            when(paymentRepository.save(any(Payment.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
        }

        private WompiTransactionRequest capturedWompiRequest() {
            ArgumentCaptor<WompiTransactionRequest> captor = ArgumentCaptor.forClass(WompiTransactionRequest.class);
            verify(wompiClient).createTransaction(captor.capture());
            return captor.getValue();
        }

        private Payment capturedPayment() {
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            return captor.getValue();
        }

        @Test
        @DisplayName("card payment: builds the Wompi request, moves the order to PROCESSING and stores the payment")
        void cardPaymentHappyPath() {
            Order order = order(OrderStatus.PENDING_PAYMENT, TOTAL);
            PaymentRequest request = cardRequest();
            WompiTransactionResponse wompi = wompiResponse(methodWithRedirect("https://bank.test/pay"));
            stubNewAttempt(order, wompi);
            PaymentResponse expected = paymentResponse();
            when(paymentMapper.toResponse(any(Payment.class))).thenReturn(expected);

            PaymentResponse result = service.createPendingPayment(orderId, request, CLIENT_IP, owner);

            assertSame(expected, result);

            WompiTransactionRequest sent = capturedWompiRequest();
            assertEquals("acceptance-token", sent.getAcceptanceToken());
            assertEquals("personal-auth-token", sent.getAcceptPersonalAuth());
            assertEquals(TOTAL_IN_CENTS, sent.getAmountInCents());
            assertEquals("COP", sent.getCurrency());
            assertEquals(owner.getEmail(), sent.getCustomerEmail());
            assertEquals("integrity-signature", sent.getSignature());
            assertSame(request.paymentMethod(), sent.getPaymentMethod());
            assertEquals(REDIRECT_URL, sent.getRedirectUrl());
            assertEquals(CLIENT_IP, sent.getIp());
            assertNull(sent.getCustomerData());
            assertTrue(sent.getReference().matches("ORDER-" + orderId + "-\\d+"),
                    "reference must be ORDER-{orderId}-{timestamp}");

            // The signature must be computed over the exact reference/amount/currency that is sent.
            verify(signatureUtil).generate(eq(sent.getReference()), eq(TOTAL_IN_CENTS), eq("COP"));

            Payment saved = capturedPayment();
            assertSame(order, saved.getOrder());
            assertEquals(TOTAL, saved.getAmount());
            assertEquals(PaymentStatus.PENDING, saved.getStatus());
            assertNull(saved.getPaymentMethod());
            assertEquals(sent.getReference(), saved.getReference());
            assertEquals(WOMPI_TX_ID, saved.getWompiTransactionId());
            assertEquals("https://bank.test/pay", saved.getRedirectUrl());
        }

        @Test
        @DisplayName("Wompi is called first, then the order moves to PROCESSING, then the payment is saved")
        void callsAreMadeInOrder() {
            stubNewAttempt(order(OrderStatus.PENDING_PAYMENT, TOTAL), wompiResponse(null));

            service.createPendingPayment(orderId, cardRequest(), CLIENT_IP, owner);

            InOrder order = inOrder(wompiClient, orderService, paymentRepository);
            order.verify(wompiClient).createTransaction(any(WompiTransactionRequest.class));
            order.verify(orderService).updateOrderStatusInternal(orderId, OrderStatus.PROCESSING);
            order.verify(paymentRepository).save(any(Payment.class));
        }

        @ParameterizedTest(name = "total {0} -> {1} cents")
        @CsvSource({
                "120000.00, 12000000",
                "1500.75,   150075",
                "0.01,      1"
        })
        @DisplayName("the order total is converted to cents by moving the decimal point two places")
        void amountIsConvertedToCents(String total, long expectedCents) {
            stubNewAttempt(order(OrderStatus.PENDING_PAYMENT, new BigDecimal(total)), wompiResponse(null));

            service.createPendingPayment(orderId, cardRequest(), CLIENT_IP, owner);

            assertEquals(expectedCents, capturedWompiRequest().getAmountInCents());
            verify(signatureUtil).generate(anyString(), eq(expectedCents), eq("COP"));
        }

        @Test
        @DisplayName("PSE with customer data: forwards full name and phone to Wompi")
        void pseForwardsCustomerData() {
            PaymentRequest request = new PaymentRequest(
                    pseBuilder().referenceOne("198.51.100.7").build(),
                    new PaymentRequest.CustomerData("Ana Pérez", "+573001234567"));
            stubNewAttempt(order(OrderStatus.PENDING_PAYMENT, TOTAL), wompiResponse(null));

            service.createPendingPayment(orderId, request, CLIENT_IP, owner);

            WompiTransactionRequest.CustomerData customer = capturedWompiRequest().getCustomerData();
            assertNotNull(customer);
            assertEquals("Ana Pérez", customer.getFullName());
            assertEquals("+573001234567", customer.getPhoneNumber());
        }

        @Test
        @DisplayName("PSE without customer data: no customer data is sent")
        void pseWithoutCustomerData() {
            PaymentRequest request = new PaymentRequest(pseBuilder().build(), null);
            stubNewAttempt(order(OrderStatus.PENDING_PAYMENT, TOTAL), wompiResponse(null));

            service.createPendingPayment(orderId, request, CLIENT_IP, owner);

            assertNull(capturedWompiRequest().getCustomerData());
        }

        @Test
        @DisplayName("card payment ignores any customer data sent by the client")
        void cardIgnoresCustomerData() {
            PaymentRequest request = new PaymentRequest(
                    CardPaymentMethod.builder().token("tok_test_123").installments(1).build(),
                    new PaymentRequest.CustomerData("Ana Pérez", "+573001234567"));
            stubNewAttempt(order(OrderStatus.PENDING_PAYMENT, TOTAL), wompiResponse(null));

            service.createPendingPayment(orderId, request, CLIENT_IP, owner);

            assertNull(capturedWompiRequest().getCustomerData());
        }

        @ParameterizedTest(name = "referenceOne [{0}] is replaced by the client IP")
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("PSE with a missing or blank referenceOne: the client IP is used")
        void pseFillsReferenceOneWithClientIp(String referenceOne) {
            PsePaymentMethod pse = pseBuilder().referenceOne(referenceOne).build();
            stubNewAttempt(order(OrderStatus.PENDING_PAYMENT, TOTAL), wompiResponse(null));

            service.createPendingPayment(orderId, new PaymentRequest(pse, null), CLIENT_IP, owner);

            PsePaymentMethod sent = (PsePaymentMethod) capturedWompiRequest().getPaymentMethod();
            assertEquals(CLIENT_IP, sent.getReferenceOne());
        }

        @Test
        @DisplayName("PSE with its own referenceOne: it is not overwritten")
        void pseKeepsProvidedReferenceOne() {
            PsePaymentMethod pse = pseBuilder().referenceOne("198.51.100.7").build();
            stubNewAttempt(order(OrderStatus.PENDING_PAYMENT, TOTAL), wompiResponse(null));

            service.createPendingPayment(orderId, new PaymentRequest(pse, null), CLIENT_IP, owner);

            PsePaymentMethod sent = (PsePaymentMethod) capturedWompiRequest().getPaymentMethod();
            assertEquals("198.51.100.7", sent.getReferenceOne());
        }

        @Test
        @DisplayName("redirect URL is null when Wompi returns no payment method")
        void redirectUrlNullWithoutPaymentMethod() {
            stubNewAttempt(order(OrderStatus.PENDING_PAYMENT, TOTAL), wompiResponse(null));

            service.createPendingPayment(orderId, cardRequest(), CLIENT_IP, owner);

            assertNull(capturedPayment().getRedirectUrl());
        }

        @Test
        @DisplayName("redirect URL is null when the Wompi payment method has no extra data")
        void redirectUrlNullWithoutExtra() {
            WompiTransactionResponse.PaymentMethodData method = new WompiTransactionResponse.PaymentMethodData();
            method.setType("CARD");
            stubNewAttempt(order(OrderStatus.PENDING_PAYMENT, TOTAL), wompiResponse(method));

            service.createPendingPayment(orderId, cardRequest(), CLIENT_IP, owner);

            assertNull(capturedPayment().getRedirectUrl());
        }

        @Test
        @DisplayName("404 – order missing in the repository: Wompi is never called")
        void orderNotFound() {
            when(paymentRepository.findActiveByOrderId(orderId)).thenReturn(Optional.empty());
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            OrderNotFoundException ex = assertThrows(OrderNotFoundException.class,
                    () -> service.createPendingPayment(orderId, cardRequest(), CLIENT_IP, owner));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
            verifyNoInteractions(wompiClient, acceptanceTokenService, signatureUtil, paymentMapper);
            verify(orderService, never()).updateOrderStatusInternal(any(), any());
            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @ParameterizedTest(name = "order in {0}")
        @EnumSource(value = OrderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PENDING_PAYMENT")
        @DisplayName("409 – only orders pending payment can start a payment: Wompi is never called")
        void orderNotPendingPayment(OrderStatus status) {
            when(paymentRepository.findActiveByOrderId(orderId)).thenReturn(Optional.empty());
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order(status, TOTAL)));

            InvalidOrderStateException ex = assertThrows(InvalidOrderStateException.class,
                    () -> service.createPendingPayment(orderId, cardRequest(), CLIENT_IP, owner));

            assertEquals(HttpStatus.CONFLICT, ex.getStatus());
            assertTrue(ex.getMessage().contains(status.name()));
            verifyNoInteractions(wompiClient, acceptanceTokenService, signatureUtil, paymentMapper);
            verify(orderService, never()).updateOrderStatusInternal(any(), any());
            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("Wompi failure: the error propagates and neither the order nor the payment change")
        void wompiFailureLeavesStateUntouched() {
            when(paymentRepository.findActiveByOrderId(orderId)).thenReturn(Optional.empty());
            when(orderRepository.findById(orderId))
                    .thenReturn(Optional.of(order(OrderStatus.PENDING_PAYMENT, TOTAL)));
            when(acceptanceTokenService.getTokens())
                    .thenReturn(new WompiAcceptanceTokens("acceptance-token", "personal-auth-token"));
            when(signatureUtil.generate(anyString(), anyLong(), anyString())).thenReturn("integrity-signature");
            RuntimeException failure = new RuntimeException("wompi down");
            when(wompiClient.createTransaction(any(WompiTransactionRequest.class))).thenThrow(failure);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.createPendingPayment(orderId, cardRequest(), CLIENT_IP, owner));

            assertSame(failure, thrown);
            verify(orderService, never()).updateOrderStatusInternal(any(), any());
            verify(paymentRepository, never()).save(any(Payment.class));
        }
    }

    // ------------------------------------------------------------------ confirmPayment

    @Nested
    @DisplayName("confirmPayment – Wompi webhook")
    class ConfirmPayment {

        private Payment stubPendingPayment(OrderStatus orderStatus) {
            Payment payment = pendingPayment(order(orderStatus, TOTAL));
            when(paymentRepository.findByReference(REFERENCE)).thenReturn(Optional.of(payment));
            return payment;
        }

        @Test
        @DisplayName("APPROVED: payment is approved and stamped, and the order becomes PAID")
        void approved() {
            Payment payment = stubPendingPayment(OrderStatus.PROCESSING);
            Instant before = Instant.now();

            service.confirmPayment(webhook("APPROVED", "CARD"));

            assertEquals(PaymentStatus.APPROVED, payment.getStatus());
            assertNotNull(payment.getPaidAt());
            assertFalse(payment.getPaidAt().isBefore(before));
            assertEquals(WOMPI_TX_ID, payment.getWompiTransactionId());
            assertEquals(PaymentMethod.CARD, payment.getPaymentMethod());
            verify(orderService).updateOrderStatusInternal(orderId, OrderStatus.PAID);
            verify(paymentRepository).save(payment);
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({"DECLINED, DECLINED", "VOIDED, VOIDED"})
        @DisplayName("DECLINED / VOIDED: payment is closed without paidAt and the order can be paid again")
        void declinedOrVoided(String wompiStatus, PaymentStatus expected) {
            Payment payment = stubPendingPayment(OrderStatus.PROCESSING);

            service.confirmPayment(webhook(wompiStatus, "CARD"));

            assertEquals(expected, payment.getStatus());
            assertNull(payment.getPaidAt());
            verify(orderService).updateOrderStatusInternal(orderId, OrderStatus.PENDING_PAYMENT);
            verify(paymentRepository).save(payment);
        }

        @ParameterizedTest(name = "status {0}")
        @ValueSource(strings = {"ERROR", "SOMETHING_UNKNOWN"})
        @DisplayName("ERROR / unrecognised status: payment is marked ERROR and the order can be paid again")
        void errorOrUnknownStatus(String wompiStatus) {
            Payment payment = stubPendingPayment(OrderStatus.PROCESSING);

            service.confirmPayment(webhook(wompiStatus, "CARD"));

            assertEquals(PaymentStatus.ERROR, payment.getStatus());
            assertNull(payment.getPaidAt());
            verify(orderService).updateOrderStatusInternal(orderId, OrderStatus.PENDING_PAYMENT);
            verify(paymentRepository).save(payment);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(PaymentMethod.class)
        @DisplayName("known payment method types are stored on the payment")
        void knownPaymentMethodIsStored(PaymentMethod method) {
            Payment payment = stubPendingPayment(OrderStatus.PROCESSING);

            service.confirmPayment(webhook("APPROVED", method.name()));

            assertEquals(method, payment.getPaymentMethod());
        }

        @Test
        @DisplayName("payment method outside the enum (e.g. BANCOLOMBIA_TRANSFER): stored as null, webhook still processed")
        void unknownPaymentMethodIsStoredAsNull() {
            Payment payment = stubPendingPayment(OrderStatus.PROCESSING);
            payment.setPaymentMethod(PaymentMethod.CARD);

            service.confirmPayment(webhook("APPROVED", "BANCOLOMBIA_TRANSFER"));

            assertNull(payment.getPaymentMethod());
            assertEquals(PaymentStatus.APPROVED, payment.getStatus());
            verify(orderService).updateOrderStatusInternal(orderId, OrderStatus.PAID);
        }

        @Test
        @DisplayName("missing payment method type: the existing payment method is kept")
        void missingPaymentMethodKeepsExisting() {
            Payment payment = stubPendingPayment(OrderStatus.PROCESSING);
            payment.setPaymentMethod(PaymentMethod.PSE);

            service.confirmPayment(webhook("APPROVED", null));

            assertEquals(PaymentMethod.PSE, payment.getPaymentMethod());
            assertEquals(PaymentStatus.APPROVED, payment.getStatus());
        }

        @ParameterizedTest(name = "payment already {0}")
        @EnumSource(value = PaymentStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PENDING")
        @DisplayName("duplicate webhook for a payment in a final state is ignored (idempotency)")
        void finalStateIsIgnored(PaymentStatus finalStatus) {
            Payment payment = stubPendingPayment(OrderStatus.PROCESSING);
            payment.setStatus(finalStatus);
            payment.setWompiTransactionId("original-tx");

            service.confirmPayment(webhook("APPROVED", "CARD"));

            assertEquals(finalStatus, payment.getStatus());
            assertEquals("original-tx", payment.getWompiTransactionId());
            assertNull(payment.getPaidAt());
            verifyNoInteractions(orderService);
            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("404 – unknown reference: nothing is updated")
        void unknownReference() {
            when(paymentRepository.findByReference(REFERENCE)).thenReturn(Optional.empty());

            PaymentNotFoundException ex = assertThrows(PaymentNotFoundException.class,
                    () -> service.confirmPayment(webhook("APPROVED", "CARD")));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
            assertTrue(ex.getMessage().contains(REFERENCE));
            verifyNoInteractions(orderService);
            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @ParameterizedTest(name = "order in {0}")
        @EnumSource(value = OrderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PROCESSING")
        @DisplayName("409 – webhook for an order that is not PROCESSING: nothing is updated")
        void orderNotProcessing(OrderStatus status) {
            Payment payment = stubPendingPayment(status);

            InvalidOrderStateException ex = assertThrows(InvalidOrderStateException.class,
                    () -> service.confirmPayment(webhook("APPROVED", "CARD")));

            assertEquals(HttpStatus.CONFLICT, ex.getStatus());
            assertEquals(PaymentStatus.PENDING, payment.getStatus());
            assertNull(payment.getWompiTransactionId());
            verifyNoInteractions(orderService);
            verify(paymentRepository, never()).save(any(Payment.class));
        }
    }

    // ------------------------------------------------------------------ getFinancialInstitutions

    @Nested
    @DisplayName("getFinancialInstitutions")
    class GetFinancialInstitutions {

        @Test
        @DisplayName("returns the institutions list received from Wompi")
        void returnsWompiData() {
            WompiFinancialInstitutionsResponse bank = new WompiFinancialInstitutionsResponse();
            bank.setCode("1");
            bank.setName("Banco que permite pagos con PSE");
            List<WompiFinancialInstitutionsResponse> institutions = List.of(bank);
            WompiFinancialInstitutionsListResponse wompi = new WompiFinancialInstitutionsListResponse();
            wompi.setData(institutions);
            when(wompiClient.getFinancialInstitutions()).thenReturn(wompi);

            List<WompiFinancialInstitutionsResponse> result = service.getFinancialInstitutions();

            assertSame(institutions, result);
            verify(wompiClient).getFinancialInstitutions();
        }
    }
}