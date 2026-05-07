package com.artelier.api.exception;

import java.util.UUID;

public class OrderNotFoundException extends ArtelierException {

    public OrderNotFoundException(UUID orderId) {
        super("Order not found: " + orderId, org.springframework.http.HttpStatus.NOT_FOUND);
    }
}