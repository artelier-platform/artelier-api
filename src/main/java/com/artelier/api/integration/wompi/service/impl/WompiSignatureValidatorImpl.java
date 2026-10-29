package com.artelier.api.integration.wompi.service.impl;

import com.artelier.api.dto.request.PaymentWebhookRequest;
import com.artelier.api.integration.wompi.service.WompiSignatureValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Slf4j
@Component
public class WompiSignatureValidatorImpl implements WompiSignatureValidator {

    @Value("${wompi.events-key}")
    private String eventsKey;

    @Override
    public boolean isValid(PaymentWebhookRequest request) {
        try {
            var tx = request.data().transaction();
            var sig = request.signature();

            StringBuilder raw = new StringBuilder();
            for (String prop : sig.properties()) {
                raw.append(resolveProperty(tx, prop));
            }
            raw.append(request.timestamp());
            raw.append(eventsKey);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);

            return computed.equals(sig.checksum());
        } catch (Exception e) {
            log.error("Error validating Wompi webhook signature: {}", e.getMessage(), e);
            return false;
        }
    }

    private String resolveProperty(
            PaymentWebhookRequest.Transaction tx,
            String prop
    ) {
        if (prop == null || prop.isBlank()) {
            throw new IllegalArgumentException(
                    "Webhook signature property cannot be null or blank"
            );
        }

        return switch (prop) {
            case "transaction.id" -> tx.id();
            case "transaction.status" -> tx.status();
            case "transaction.amount_in_cents" ->
                    String.valueOf(tx.amountInCents());
            default -> throw new IllegalArgumentException(
                    "Unknown webhook signature property: " + prop
            );
        };
    }
}