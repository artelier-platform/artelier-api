package com.artelier.api.service;

import com.artelier.api.dto.request.PaymentRequest;
import com.artelier.api.dto.response.PaymentResponse;
import com.artelier.api.entity.Payment;
import com.artelier.api.entity.User;
import com.artelier.api.enums.Role;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.exception.PaymentNotFoundException;
import com.artelier.api.integration.wompi.dto.request.CardPaymentMethod;
import com.artelier.api.integration.wompi.enums.PaymentStatus;
import com.artelier.api.integration.wompi.service.WompiAcceptanceTokenService;
import com.artelier.api.integration.wompi.service.WompiClient;
import com.artelier.api.integration.wompi.util.WompiIntegritySignatureUtil;
import com.artelier.api.mapper.PaymentMapper;
import com.artelier.api.repository.OrderRepository;
import com.artelier.api.repository.PaymentRepository;
import com.artelier.api.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Order-ownership rules of {@link PaymentServiceImpl}: a payment can only be created or read by
 * the owner of the order (or an admin). The rule itself lives in {@link OrderService#getOrderById},
 * so these tests check that the payment service applies it first, with the right requester, and
 * stops before touching payments or Wompi when it fails.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl – order ownership")
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

    private final UUID orderId = UUID.randomUUID();
    private final User owner = user(Role.BUYER);
    private final User stranger = user(Role.BUYER);

    private static User user(Role role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(UUID.randomUUID() + "@artelier.com");
        user.setRole(role);
        return user;
    }

    private static PaymentRequest cardRequest() {
        return new PaymentRequest(
                CardPaymentMethod.builder().token("tok_test_123").installments(1).build(), null);
    }

    private PaymentResponse response() {
        return new PaymentResponse(
                UUID.randomUUID(), orderId, null, "ORDER-" + orderId, PaymentStatus.PENDING,
                new BigDecimal("120000.00"), null, null, null);
    }

    // ------------------------------------------------------------------ findByOrderId

    @Nested
    @DisplayName("findByOrderId")
    class FindByOrderId {

        @Test
        @DisplayName("owner reads the payment: ownership is checked before the payment is loaded")
        void ownerReadsPayment() {
            Payment payment = Payment.builder().build();
            PaymentResponse response = response();
            when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
            when(paymentMapper.toResponse(payment)).thenReturn(response);

            assertSame(response, service.findByOrderId(orderId, owner));

            InOrder order = inOrder(orderService, paymentRepository);
            order.verify(orderService).getOrderById(orderId, owner);
            order.verify(paymentRepository).findByOrderId(orderId);
        }

        @Test
        @DisplayName("403 – order of another user: the payment is never loaded")
        void foreignOrderIsForbidden() {
            when(orderService.getOrderById(orderId, stranger))
                    .thenThrow(ArtelierException.forbidden("Access denied to this order"));

            ArtelierException ex = assertThrows(ArtelierException.class,
                    () -> service.findByOrderId(orderId, stranger));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertEquals("Access denied to this order", ex.getMessage());
            verifyNoInteractions(paymentRepository, paymentMapper);
        }

        @Test
        @DisplayName("404 – order does not exist: the payment is never loaded")
        void unknownOrderIsNotFound() {
            when(orderService.getOrderById(orderId, owner))
                    .thenThrow(ArtelierException.notFound("Order not found"));

            ArtelierException ex = assertThrows(ArtelierException.class,
                    () -> service.findByOrderId(orderId, owner));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
            verifyNoInteractions(paymentRepository, paymentMapper);
        }

        @Test
        @DisplayName("404 – owner of an order that has no payment yet")
        void ownerWithoutPayment() {
            when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

            assertThrows(PaymentNotFoundException.class, () -> service.findByOrderId(orderId, owner));

            verify(orderService).getOrderById(orderId, owner);
            verifyNoInteractions(paymentMapper);
        }
    }

    // ------------------------------------------------------------------ createPendingPayment

    @Nested
    @DisplayName("createPendingPayment")
    class CreatePendingPayment {

        @Test
        @DisplayName("owner with an existing pending payment gets it back without calling Wompi")
        void ownerGetsExistingPendingPayment() {
            Payment pending = Payment.builder().build();
            PaymentResponse response = response();
            when(paymentRepository.findActiveByOrderId(orderId)).thenReturn(Optional.of(pending));
            when(paymentMapper.toResponse(pending)).thenReturn(response);

            assertSame(response, service.createPendingPayment(orderId, cardRequest(), "203.0.113.5", owner));

            InOrder order = inOrder(orderService, paymentRepository);
            order.verify(orderService).getOrderById(orderId, owner);
            order.verify(paymentRepository).findActiveByOrderId(orderId);
            verifyNoInteractions(wompiClient, acceptanceTokenService, signatureUtil, orderRepository);
        }

        @Test
        @DisplayName("403 – order of another user: no lookup, no Wompi call, the pending payment is not exposed")
        void foreignOrderIsForbidden() {
            when(orderService.getOrderById(orderId, stranger))
                    .thenThrow(ArtelierException.forbidden("Access denied to this order"));

            ArtelierException ex = assertThrows(ArtelierException.class,
                    () -> service.createPendingPayment(orderId, cardRequest(), "203.0.113.5", stranger));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verifyNoInteractions(paymentRepository, orderRepository, paymentMapper,
                    wompiClient, acceptanceTokenService, signatureUtil);
        }

        @Test
        @DisplayName("404 – order does not exist: nothing else is touched")
        void unknownOrderIsNotFound() {
            when(orderService.getOrderById(orderId, owner))
                    .thenThrow(ArtelierException.notFound("Order not found"));

            ArtelierException ex = assertThrows(ArtelierException.class,
                    () -> service.createPendingPayment(orderId, cardRequest(), "203.0.113.5", owner));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
            verifyNoInteractions(paymentRepository, orderRepository, wompiClient,
                    acceptanceTokenService, signatureUtil);
        }
    }
}
