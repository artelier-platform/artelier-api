package com.artelier.api.payment.exception;

import java.util.UUID;
import com.artelier.api.payment.entity.Payment;
import com.artelier.api.shared.exception.ArtelierException;

public class PaymentNotFoundException extends ArtelierException {

    public PaymentNotFoundException(String reference) {
        super("Payment not found for reference: " + reference, org.springframework.http.HttpStatus.NOT_FOUND);
    }

    public PaymentNotFoundException(UUID orderId) {
        super("No payment found for order: " + orderId, org.springframework.http.HttpStatus.NOT_FOUND);
    }
}