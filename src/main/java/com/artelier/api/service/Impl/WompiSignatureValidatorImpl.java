package com.artelier.api.service.Impl;

import com.artelier.api.dto.request.PaymentWebhookRequest;
import com.artelier.api.service.WompiSignatureValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class WompiSignatureValidatorImpl implements WompiSignatureValidator {

    @Value("${wompi.events-key}")
    private String eventsKey;

    @Override
    public boolean isValid(PaymentWebhookRequest request) {
        try {
            var tx = request.getData().getTransaction();
            var sig = request.getSignature();

            StringBuilder raw = new StringBuilder();
            for (String prop : sig.getProperties()) {
                raw.append(resolveProperty(tx, prop));
            }
            raw.append(request.getTimestamp());
            raw.append(eventsKey);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);

            return computed.equals(sig.getChecksum());
        } catch (Exception e) {
            return false;
        }
    }

    private String resolveProperty(PaymentWebhookRequest.Transaction tx, String prop) {
        return switch (prop) {
            case "transaction.id" -> tx.getId();
            case "transaction.status" -> tx.getStatus();
            case "transaction.amount_in_cents" -> String.valueOf(tx.getAmountInCents());
            default -> "";
        };
    }
}