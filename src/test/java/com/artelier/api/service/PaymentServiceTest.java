package com.artelier.api.service;

import com.artelier.api.dto.request.PaymentWebhookRequest;
import com.artelier.api.dto.response.PaymentResponse;
import com.artelier.api.entity.Order;
import com.artelier.api.entity.Payment;
import com.artelier.api.entity.enums.OrderStatus;
import com.artelier.api.entity.enums.PaymentMethod;
import com.artelier.api.entity.enums.PaymentStatus;
import com.artelier.api.exception.InvalidOrderStateException;
import com.artelier.api.exception.OrderNotFoundException;
import com.artelier.api.exception.PaymentNotFoundException;
import com.artelier.api.mapper.PaymentMapper;
import com.artelier.api.repository.OrderRepository;
import com.artelier.api.repository.PaymentRepository;
import com.artelier.api.service.Impl.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentServiceImpl service;

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
        verifyNoInteractions(orderService);
    }

    @Test
    void shouldSetStatusErrorForUnknownWompiStatus() {
        Payment payment = buildPayment(PaymentStatus.PENDING, OrderStatus.PROCESSING);

        when(paymentRepository.findByReference("ref-123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        service.confirmPayment(buildWebhookRequest("ref-123", "VOIDED", "CARD"));

        assertEquals(PaymentStatus.ERROR, payment.getStatus());
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
    }

    @Test
    void shouldThrowIfPaymentNotFoundOnConfirm() {
        when(paymentRepository.findByReference("ref-xyz"))
                .thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class,
                () -> service.confirmPayment(buildWebhookRequest("ref-xyz", "APPROVED", "NEQUI")));
    }

    @Test
    void shouldThrowIfOrderNotInProcessingOnConfirm() {
        Payment payment = buildPayment(PaymentStatus.PENDING, OrderStatus.CANCELLED);

        when(paymentRepository.findByReference("ref-123"))
                .thenReturn(Optional.of(payment));

        assertThrows(InvalidOrderStateException.class,
                () -> service.confirmPayment(buildWebhookRequest("ref-123", "APPROVED", "NEQUI")));
    }

    // ─── createPendingPayment ─────────────────────────────────────────────────

    @Test
    void shouldCreatePendingPayment() {
        UUID orderId = UUID.randomUUID();
        Order order = buildOrder(orderId, OrderStatus.PENDING_PAYMENT);
        Payment saved = new Payment();
        PaymentResponse response = new PaymentResponse();

        when(paymentRepository.findByReference(orderId.toString()))
                .thenReturn(Optional.empty());
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));
        when(paymentRepository.save(any()))
                .thenReturn(saved);
        when(paymentMapper.toResponse(saved))
                .thenReturn(response);

        PaymentResponse result = service.createPendingPayment(orderId);

        assertNotNull(result);
        verify(paymentRepository).save(any());
    }

    @Test
    void shouldReturnExistingPaymentIfAlreadyCreated() {
        UUID orderId = UUID.randomUUID();
        Payment existing = new Payment();
        PaymentResponse response = new PaymentResponse();

        when(paymentRepository.findByReference(orderId.toString()))
                .thenReturn(Optional.of(existing));
        when(paymentMapper.toResponse(existing))
                .thenReturn(response);

        PaymentResponse result = service.createPendingPayment(orderId);

        assertNotNull(result);
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).findById(any());
    }

    @Test
    void shouldThrowIfOrderNotFoundOnCreate() {
        UUID orderId = UUID.randomUUID();

        when(paymentRepository.findByReference(orderId.toString()))
                .thenReturn(Optional.empty());
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> service.createPendingPayment(orderId));
    }

    @Test
    void shouldThrowIfOrderNotInProcessingOnCreate() {
        UUID orderId = UUID.randomUUID();
        Order order = buildOrder(orderId, OrderStatus.CANCELLED);

        when(paymentRepository.findByReference(orderId.toString()))
                .thenReturn(Optional.empty());
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class,
                () -> service.createPendingPayment(orderId));
    }

    // ─── findByOrderId ────────────────────────────────────────────────────────

    @Test
    void shouldFindPaymentByOrderId() {
        UUID orderId = UUID.randomUUID();
        Payment payment = new Payment();
        PaymentResponse response = new PaymentResponse();

        when(paymentRepository.findByReference(orderId.toString()))
                .thenReturn(Optional.of(payment));
        when(paymentMapper.toResponse(payment))
                .thenReturn(response);

        PaymentResponse result = service.findByOrderId(orderId);

        assertNotNull(result);
    }

    @Test
    void shouldThrowIfPaymentNotFoundByOrderId() {
        UUID orderId = UUID.randomUUID();

        when(paymentRepository.findByReference(orderId.toString()))
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
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setTotal(BigDecimal.valueOf(120000));
        return order;
    }

    private PaymentWebhookRequest buildWebhookRequest(String reference, String status, String method) {
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