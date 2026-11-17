package com.artelier.api.order.exception;

import java.util.UUID;
import com.artelier.api.order.entity.Order;
import com.artelier.api.shared.exception.ArtelierException;

public class OrderNotFoundException extends ArtelierException {

    public OrderNotFoundException(UUID orderId) {
        super("Order not found: " + orderId, org.springframework.http.HttpStatus.NOT_FOUND);
    }
}