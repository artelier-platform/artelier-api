package com.artelier.api.exception;

import java.util.UUID;

public class PaymentNotFoundException extends ArtelierException {

    public PaymentNotFoundException(String reference) {
        super("Payment not found for reference: " + reference, org.springframework.http.HttpStatus.NOT_FOUND);
    }

    public PaymentNotFoundException(UUID orderId) {
        super("No payment found for order: " + orderId, org.springframework.http.HttpStatus.NOT_FOUND);
    }
}