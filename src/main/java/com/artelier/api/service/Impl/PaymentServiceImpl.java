package com.artelier.api.service.Impl;

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
import com.artelier.api.service.OrderService;
import com.artelier.api.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public void confirmPayment(PaymentWebhookRequest request) {
        var tx = request.getData().getTransaction();

        Payment payment = paymentRepository.findByReference(tx.getReference())
                .orElseThrow(() -> new PaymentNotFoundException(tx.getReference()));

        if (payment.getStatus() == PaymentStatus.APPROVED
                || payment.getStatus() == PaymentStatus.DECLINED) {
            return;
        }

        Order order = payment.getOrder();
        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new InvalidOrderStateException(order.getStatus());
        }

        payment.setWompiTransactionId(tx.getId());

        try {
            payment.setPaymentMethod(PaymentMethod.valueOf(tx.getPaymentMethodType()));
        } catch (IllegalArgumentException e) {
            payment.setPaymentMethod(null);
        }

        if ("APPROVED".equals(tx.getStatus())) {
            payment.setStatus(PaymentStatus.APPROVED);
            payment.setPaidAt(Instant.now());
            orderService.updateOrderStatus(order.getId(), OrderStatus.PAID);
        } else if ("DECLINED".equals(tx.getStatus())) {
            payment.setStatus(PaymentStatus.DECLINED);
        } else {
            payment.setStatus(PaymentStatus.ERROR);
        }

        paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public PaymentResponse createPendingPayment(UUID orderId) {
        return paymentRepository.findByReference(orderId.toString())
                .map(paymentMapper::toResponse)
                .orElseGet(() -> {
                    Order order = orderRepository.findById(orderId)
                            .orElseThrow(() -> new OrderNotFoundException(orderId));

                    if (order.getStatus() != OrderStatus.PROCESSING) {
                        throw new InvalidOrderStateException(order.getStatus());
                    }

                    Payment payment = Payment.builder()
                            .order(order)
                            .amount(order.getTotal())
                            .status(PaymentStatus.PENDING)
                            .paymentMethod(null)
                            .reference(orderId.toString())
                            .wompiTransactionId(null)
                            .build();

                    return paymentMapper.toResponse(paymentRepository.save(payment));
                });
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse findByOrderId(UUID orderId) {
        return paymentRepository.findByReference(orderId.toString())
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
    }
}
