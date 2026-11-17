package com.artelier.api.order.exception;

import com.artelier.api.order.enums.OrderStatus;
import com.artelier.api.shared.exception.ArtelierException;

public class InvalidOrderStateException extends ArtelierException {

    public InvalidOrderStateException(OrderStatus current) {
        super(
                "Cannot process payment for order with status: " + current,
                org.springframework.http.HttpStatus.CONFLICT
        );
    }
}