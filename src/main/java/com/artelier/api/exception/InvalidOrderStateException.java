package com.artelier.api.exception;

import com.artelier.api.entity.enums.OrderStatus;

public class InvalidOrderStateException extends ArtelierException {

    public InvalidOrderStateException(OrderStatus current) {
        super(
                "Cannot process payment for order with status: " + current,
                org.springframework.http.HttpStatus.CONFLICT
        );
    }
}