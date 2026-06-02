package com.artelier.api.service;

import com.artelier.api.dto.request.PaymentRequest;
import com.artelier.api.entity.User;
import com.artelier.api.integration.wompi.dto.request.CardPaymentMethod;
import com.artelier.api.integration.wompi.dto.request.PaymentWebhookRequest;
import com.artelier.api.integration.wompi.dto.request.WompiAcceptanceTokens;
import com.artelier.api.integration.wompi.dto.response.WompiTransactionResponse;
import com.artelier.api.integration.wompi.service.WompiAcceptanceTokenService;
import com.artelier.api.integration.wompi.service.WompiClient;
import com.artelier.api.integration.wompi.util.WompiIntegritySignatureUtil;
import com.artelier.api.dto.response.PaymentResponse;
import com.artelier.api.entity.Order;
import com.artelier.api.entity.Payment;
import com.artelier.api.enums.OrderStatus;
import com.artelier.api.integration.wompi.enums.PaymentMethod;
import com.artelier.api.integration.wompi.enums.PaymentStatus;
import com.artelier.api.exception.InvalidOrderStateException;
import com.artelier.api.exception.OrderNotFoundException;
import com.artelier.api.exception.PaymentNotFoundException;
import com.artelier.api.mapper.PaymentMapper;
import com.artelier.api.repository.OrderRepository;
import com.artelier.api.repository.PaymentRepository;
import com.artelier.api.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderService orderService;
    @Mock private PaymentMapper paymentMapper;
    @Mock private WompiClient wompiClient;
    @Mock private WompiAcceptanceTokenService acceptanceTokenService;
    @Mock private WompiIntegritySignatureUtil signatureUtil;

    @InjectMocks
    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        // @Value no se inyecta con @InjectMocks — hay que setearlo manualmente
        ReflectionTestUtils.setField(service, "redirectUrl",
                "http://localhost:3000/payment/result");
    }

    // ─── confirmPayment ───────────────────────────────────────────────────────

    @Test
    void shouldConfirmPaymentAsApproved() {
        Payment payment = buildPayment(PaymentStatus.PENDING, OrderStatus.PROCESSING);

        when(paymentRepository.findByReference("ref-123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        service.confirmPayment(buildWebhookRequest("ref-123", "APPROVED", "NEQUI"));

        assertEquals(PaymentStatus.APPROVED, payment.getStatus());
        assertNotNull(payment.getPaidAt());
        assertEquals(PaymentMethod.NEQUI, payment.getPaymentMethod());
        verify(orderService).updateOrderStatus(payment.getOrder().getId(), OrderStatus.PAID);
        verify(paymentRepository).save(payment);
    }

    @Test
    void shouldConfirmPaymentAsDeclined() {
        Payment payment = buildPayment(PaymentStatus.PENDING, OrderStatus.PROCESSING);

        when(paymentRepository.findByReference("ref-123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        service.confirmPayment(buildWebhookRequest("ref-123", "DECLINED", "PSE"));

        assertEquals(PaymentStatus.DECLINED, payment.getStatus());
        assertNull(payment.getPaidAt());
        verify(orderService).updateOrderStatus(payment.getOrder().getId(), OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void shouldConfirmPaymentAsVoided() {
        Payment payment = buildPayment(PaymentStatus.PENDING, OrderStatus.PROCESSING);

        when(paymentRepository.findByReference("ref-123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        service.confirmPayment(buildWebhookRequest("ref-123", "VOIDED", "CARD"));

        // VOIDED es un estado propio, no ERROR
        assertEquals(PaymentStatus.VOIDED, payment.getStatus());
        verify(orderService).updateOrderStatus(payment.getOrder().getId(), OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void shouldSetStatusErrorForTrulyUnknownStatus() {
        Payment payment = buildPayment(PaymentStatus.PENDING, OrderStatus.PROCESSING);

        when(paymentRepository.findByReference("ref-123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        // status que Wompi no debería mandar pero que el switch default captura
        service.confirmPayment(buildWebhookRequest("ref-123", "UNKNOWN_STATUS", "CARD"));

        assertEquals(PaymentStatus.ERROR, payment.getStatus());
        verify(orderService).updateOrderStatus(payment.getOrder().getId(), OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void shouldSetPaymentMethodNullForUnknownMethod() {
        Payment payment = buildPayment(PaymentStatus.PENDING, OrderStatus.PROCESSING);

        when(paymentRepository.findByReference("ref-123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        service.confirmPayment(buildWebhookRequest("ref-123", "APPROVED", "CRYPTO"));

        assertNull(payment.getPaymentMethod());
    }

    @Test
    void shouldSkipConfirmIfAlreadyApproved() {
        Payment payment = buildPayment(PaymentStatus.APPROVED, OrderStatus.PAID);

        when(paymentRepository.findByReference("ref-123"))
                .thenReturn(Optional.of(payment));

        service.confirmPayment(buildWebhookRequest("ref-123", "APPROVED", "NEQUI"));

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(orderService);
    }

    @Test
    void shouldSkipConfirmIfAlreadyDeclined() {
        Payment payment = buildPayment(PaymentStatus.DECLINED, OrderStatus.PROCESSING);

        when(paymentRepository.findByReference("ref-123"))
                .thenReturn(Optional.of(payment));

        service.confirmPayment(buildWebhookRequest("ref-123", "APPROVED", "NEQUI"));

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(orderService);
    }

    @Test
    void shouldThrowIfPaymentNotFoundOnConfirm() {
        when(paymentRepository.findByReference("ref-xyz"))
                .thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class,
                () -> service.confirmPayment(
                        buildWebhookRequest("ref-xyz", "APPROVED", "NEQUI")));
    }

    @Test
    void shouldThrowIfOrderNotInProcessingOnConfirm() {
        Payment payment = buildPayment(PaymentStatus.PENDING, OrderStatus.CANCELLED);

        when(paymentRepository.findByReference("ref-123"))
                .thenReturn(Optional.of(payment));

        assertThrows(InvalidOrderStateException.class,
                () -> service.confirmPayment(
                        buildWebhookRequest("ref-123", "APPROVED", "NEQUI")));
    }

    // ─── createPendingPayment ─────────────────────────────────────────────────

    @Test
    void shouldCreatePendingPayment() {
        UUID orderId = UUID.randomUUID();
        Order order = buildOrder(orderId, OrderStatus.PENDING_PAYMENT);
        Payment saved = new Payment();
        PaymentResponse response = new PaymentResponse();

        when(paymentRepository.findActiveByOrderId(orderId))
                .thenReturn(Optional.empty());
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));
        when(acceptanceTokenService.getTokens())
                .thenReturn(new WompiAcceptanceTokens("acceptance_token", "personal_auth"));
        when(signatureUtil.generate(anyString(), anyLong(), anyString()))
                .thenReturn("test-signature");
        when(wompiClient.createTransaction(any()))
                .thenReturn(buildWompiTransactionResponse("wompi_tx_001"));
        when(paymentRepository.save(any()))
                .thenReturn(saved);
        when(paymentMapper.toResponse(saved))
                .thenReturn(response);

        PaymentResponse result = service.createPendingPayment(
                orderId, buildCardRequest(), "192.168.1.1");

        assertNotNull(result);
        verify(paymentRepository).save(any());
        verify(orderService).updateOrderStatus(orderId, OrderStatus.PROCESSING);
        verify(wompiClient).createTransaction(any());
    }

    @Test
    void shouldReturnExistingPaymentIfAlreadyCreated() {
        UUID orderId = UUID.randomUUID();
        Payment existing = new Payment();
        PaymentResponse response = new PaymentResponse();

        // findActiveByOrderId — el método correcto
        when(paymentRepository.findActiveByOrderId(orderId))
                .thenReturn(Optional.of(existing));
        when(paymentMapper.toResponse(existing))
                .thenReturn(response);

        PaymentResponse result = service.createPendingPayment(
                orderId, buildCardRequest(), "192.168.1.1");

        assertNotNull(result);
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).findById(any());
        verifyNoInteractions(wompiClient);
    }

    @Test
    void shouldThrowIfOrderNotFoundOnCreate() {
        UUID orderId = UUID.randomUUID();

        when(paymentRepository.findActiveByOrderId(orderId))
                .thenReturn(Optional.empty());
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> service.createPendingPayment(
                        orderId, buildCardRequest(), "192.168.1.1"));
    }

    @Test
    void shouldThrowIfOrderNotInPendingPaymentOnCreate() {
        UUID orderId = UUID.randomUUID();
        Order order = buildOrder(orderId, OrderStatus.CANCELLED);

        when(paymentRepository.findActiveByOrderId(orderId))
                .thenReturn(Optional.empty());
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class,
                () -> service.createPendingPayment(
                        orderId, buildCardRequest(), "192.168.1.1"));
    }

    // ─── findByOrderId ────────────────────────────────────────────────────────

    @Test
    void shouldFindPaymentByOrderId() {
        UUID orderId = UUID.randomUUID();
        Payment payment = new Payment();
        PaymentResponse response = new PaymentResponse();

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(payment));
        when(paymentMapper.toResponse(payment))
                .thenReturn(response);

        PaymentResponse result = service.findByOrderId(orderId);

        assertNotNull(result);
    }

    @Test
    void shouldThrowIfPaymentNotFoundByOrderId() {
        UUID orderId = UUID.randomUUID();

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class,
                () -> service.findByOrderId(orderId));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Payment buildPayment(PaymentStatus paymentStatus, OrderStatus orderStatus) {
        Order order = buildOrder(UUID.randomUUID(), orderStatus);
        Payment payment = new Payment();
        payment.setStatus(paymentStatus);
        payment.setOrder(order);
        return payment;
    }

    private Order buildOrder(UUID id, OrderStatus status) {
        User user = new User();
        user.setEmail("test@artelier.com");

        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setTotal(BigDecimal.valueOf(120000));
        order.setUser(user);
        return order;
    }

    private PaymentRequest buildCardRequest() {
        PaymentRequest request = new PaymentRequest();
        request.setPaymentMethod(
                CardPaymentMethod.builder()
                        .token("tok_test_123456")
                        .installments(1)
                        .build()
        );
        return request;
    }

    private WompiTransactionResponse buildWompiTransactionResponse(String txId) {
        WompiTransactionResponse.TransactionData txData =
                new WompiTransactionResponse.TransactionData();
        txData.setId(txId);
        txData.setStatus("PENDING");
        // sin paymentMethod.extra → async_payment_url = null (caso CARD/NEQUI)

        WompiTransactionResponse response = new WompiTransactionResponse();
        response.setData(txData);
        return response;
    }

    private PaymentWebhookRequest buildWebhookRequest(
            String reference, String status, String method) {

        PaymentWebhookRequest.Transaction tx = new PaymentWebhookRequest.Transaction();
        tx.setId("wompi_tx_001");
        tx.setReference(reference);
        tx.setStatus(status);
        tx.setPaymentMethodType(method);
        tx.setAmountInCents(12000000L);

        PaymentWebhookRequest.TransactionData data = new PaymentWebhookRequest.TransactionData();
        data.setTransaction(tx);

        PaymentWebhookRequest request = new PaymentWebhookRequest();
        request.setData(data);
        return request;
    }
}