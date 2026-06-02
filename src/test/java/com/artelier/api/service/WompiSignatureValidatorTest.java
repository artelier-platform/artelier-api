package com.artelier.api.service;

import com.artelier.api.integration.wompi.dto.request.PaymentWebhookRequest;
import com.artelier.api.integration.wompi.service.impl.WompiSignatureValidatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WompiSignatureValidatorTest {

    private WompiSignatureValidatorImpl validator;

    private static final String TEST_EVENTS_KEY = "test-secret-key";

    @BeforeEach
    void setUp() {
        validator = new WompiSignatureValidatorImpl();
        ReflectionTestUtils.setField(validator, "eventsKey", TEST_EVENTS_KEY);
    }

    @Test
    void shouldReturnTrueForValidSignature() {
        String txId = "wompi_tx_001";
        String txStatus = "APPROVED";
        Long amountInCents = 12000000L;
        Long timestamp = 1715000000L;

        String checksum = computeChecksum(txId, txStatus, amountInCents, timestamp, TEST_EVENTS_KEY);

        PaymentWebhookRequest request = buildRequest(txId, txStatus, amountInCents, timestamp, checksum);

        assertTrue(validator.isValid(request));
    }

    @Test
    void shouldReturnFalseIfChecksumDoesNotMatch() {
        PaymentWebhookRequest request = buildRequest(
                "wompi_tx_001", "APPROVED", 12000000L, 1715000000L, "checksum-invalido"
        );

        assertFalse(validator.isValid(request));
    }

    @Test
    void shouldReturnFalseIfEventsKeyIsWrong() {
        // Calcula el checksum con una key diferente
        String checksum = computeChecksum("wompi_tx_001", "APPROVED", 12000000L, 1715000000L, "wrong-key");

        PaymentWebhookRequest request = buildRequest(
                "wompi_tx_001", "APPROVED", 12000000L, 1715000000L, checksum
        );

        assertFalse(validator.isValid(request));
    }

    @Test
    void shouldReturnFalseIfRequestDataIsNull() {
        PaymentWebhookRequest request = new PaymentWebhookRequest();
        request.setData(null);

        assertFalse(validator.isValid(request));
    }

    @Test
    void shouldReturnFalseIfTransactionIsNull() {
        PaymentWebhookRequest.TransactionData data = new PaymentWebhookRequest.TransactionData();
        data.setTransaction(null);

        PaymentWebhookRequest request = new PaymentWebhookRequest();
        request.setData(data);

        assertFalse(validator.isValid(request));
    }

    @Test
    void shouldReturnFalseIfSignatureIsNull() {
        PaymentWebhookRequest request = buildRequest("id", "APPROVED", 100L, 123L, "checksum");
        request.setSignature(null);

        assertFalse(validator.isValid(request));
    }

    @Test
    void shouldIgnoreUnknownPropertyAndFailValidation() {
        // "transaction.unknown" no está en resolveProperty → aporta "" al hash → no coincide
        String txId = "wompi_tx_001";
        String txStatus = "APPROVED";
        Long amountInCents = 12000000L;
        Long timestamp = 1715000000L;

        String checksum = computeChecksum(txId, txStatus, amountInCents, timestamp, TEST_EVENTS_KEY);

        PaymentWebhookRequest.Transaction tx = new PaymentWebhookRequest.Transaction();
        tx.setId(txId);
        tx.setStatus(txStatus);
        tx.setAmountInCents(amountInCents);

        PaymentWebhookRequest.SignatureData sig = new PaymentWebhookRequest.SignatureData();
        sig.setChecksum(checksum);
        // Añade una propiedad desconocida — el hash resultante no va a coincidir
        sig.setProperties(List.of("transaction.id", "transaction.status", "transaction.unknown"));

        PaymentWebhookRequest.TransactionData data = new PaymentWebhookRequest.TransactionData();
        data.setTransaction(tx);

        PaymentWebhookRequest request = new PaymentWebhookRequest();
        request.setData(data);
        request.setSignature(sig);
        request.setTimestamp(timestamp);

        assertFalse(validator.isValid(request));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private PaymentWebhookRequest buildRequest(
            String txId, String status, Long amountInCents, Long timestamp, String checksum
    ) {
        PaymentWebhookRequest.Transaction tx = new PaymentWebhookRequest.Transaction();
        tx.setId(txId);
        tx.setStatus(status);
        tx.setAmountInCents(amountInCents);

        PaymentWebhookRequest.SignatureData sig = new PaymentWebhookRequest.SignatureData();
        sig.setChecksum(checksum);
        sig.setProperties(List.of(
                "transaction.id",
                "transaction.status",
                "transaction.amount_in_cents"
        ));

        PaymentWebhookRequest.TransactionData data = new PaymentWebhookRequest.TransactionData();
        data.setTransaction(tx);

        PaymentWebhookRequest request = new PaymentWebhookRequest();
        request.setData(data);
        request.setSignature(sig);
        request.setTimestamp(timestamp);
        return request;
    }

    private String computeChecksum(
            String txId, String txStatus, Long amountInCents, Long timestamp, String key
    ) {
        try {
            String raw = txId + txStatus + amountInCents + timestamp + key;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}