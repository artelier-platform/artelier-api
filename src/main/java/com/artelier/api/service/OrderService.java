package com.artelier.api.service;

import com.artelier.api.dto.request.OrderRequest;
import com.artelier.api.dto.response.OrderResponse;
import com.artelier.api.entity.User;
import com.artelier.api.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(OrderRequest request, String userEmail);

    List<OrderResponse> getMyOrders(String userEmail);

    Page<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable, User requester);

    OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus, User requester);

    void updateOrderStatusInternal(UUID orderId, OrderStatus newStatus);

    OrderResponse getOrderById(UUID orderId, User requester);
}
