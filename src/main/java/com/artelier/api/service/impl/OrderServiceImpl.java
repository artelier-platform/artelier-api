package com.artelier.api.service.impl;

import com.artelier.api.dto.request.OrderRequest;
import com.artelier.api.dto.response.OrderResponse;
import com.artelier.api.entity.Order;
import com.artelier.api.entity.OrderItem;
import com.artelier.api.entity.Product;
import com.artelier.api.entity.User;
import com.artelier.api.enums.OrderStatus;
import com.artelier.api.enums.Role;
import com.artelier.api.enums.StockType;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.mapper.OrderMapper;
import com.artelier.api.repository.OrderRepository;
import com.artelier.api.repository.ProductRepository;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final String ORDER_NOT_FOUND = "Order not found";

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> ArtelierException.notFound("User not found"));

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING_PAYMENT)
                .shippingAddress(request.getShippingAddress())
                .notes(request.getNotes())
                .customerEmail(userEmail)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> ArtelierException.notFound("Product not found"));

            if (!product.getIsActive()) throw ArtelierException.badRequest("Product is not available");

            if (product.getStockType() != StockType.UNLIMITED) {
                int available = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
                if (available < itemRequest.getQuantity()) {
                    throw ArtelierException.badRequest(
                            "Insufficient stock for product: " + product.getName()
                                    + " (available: " + available + ")"
                    );
                }
                product.setStockQuantity(available - itemRequest.getQuantity());
            }

            BigDecimal unitPrice = product.getPrice();
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            subtotal = subtotal.add(itemTotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .customNotes(itemRequest.getCustomNotes())
                    .build();

            order.getItems().add(orderItem);
        }

        order.setSubtotal(subtotal);
        order.setTotal(subtotal);

        Order savedOrder = orderRepository.save(order);

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String userEmail) {

        return orderRepository.findByUserEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable, User requester) {

        boolean isAdmin = requester.getRole() == Role.ADMIN;

        Page<Order> orders;

        if (isAdmin) {
            orders = (status != null)
                    ? orderRepository.findByStatus(status, pageable)
                    : orderRepository.findAll(pageable);
        } else {
            orders = (status != null)
                    ? orderRepository.findByUserAndStatus(requester, status, pageable)
                    : orderRepository.findByUser(requester, pageable);
        }

        return orders.map(orderMapper::toResponse);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus, User requester) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ArtelierException.notFound(ORDER_NOT_FOUND));

        boolean isAdmin = requester.getRole() == Role.ADMIN;

        if (isAdmin) {
            if (newStatus != OrderStatus.SHIPPED) {
                throw ArtelierException.badRequest("Admin can only transition orders to SHIPPED");
            }
            if (order.getStatus() != OrderStatus.PAID) {
                throw ArtelierException.badRequest(
                        "Order must be in PAID status to be shipped, current status: " + order.getStatus()
                );
            }
        } else {
            if (newStatus != OrderStatus.CANCELLED) {
                throw ArtelierException.badRequest("You can only cancel your order");
            }
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                throw ArtelierException.badRequest(
                        "Order can only be cancelled when pending payment, current status: " + order.getStatus()
                );
            }
            if (!order.getUser().getId().equals(requester.getId())) {
                throw ArtelierException.forbidden("Access denied to this order");
            }
        }

        order.setStatus(newStatus);

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public void updateOrderStatusInternal(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ArtelierException.notFound(ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw ArtelierException.badRequest("Cannot modify a cancelled order");
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId, User requester) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ArtelierException.notFound(ORDER_NOT_FOUND));

        boolean isAdmin = requester.getRole() == Role.ADMIN;

        if (!isAdmin && !order.getUser().getId().equals(requester.getId())) {
            throw ArtelierException.forbidden("Access denied to this order");
        }

        return orderMapper.toResponse(order);
    }
}
