package com.artelier.api.integration.wompi.service;

import com.artelier.api.dto.request.PaymentWebhookRequest;
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

    // ─── Happy path ───────────────────────────────────────────────────────────

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
    void shouldReturnTrueForDeclinedTransaction() {
        String txId = "wompi_tx_002";
        String txStatus = "DECLINED";
        Long amountInCents = 5000000L;
        Long timestamp = 1715000001L;

        String checksum = computeChecksum(txId, txStatus, amountInCents, timestamp, TEST_EVENTS_KEY);
        PaymentWebhookRequest request = buildRequest(txId, txStatus, amountInCents, timestamp, checksum);

        assertTrue(validator.isValid(request));
    }

    @Test
    void shouldReturnTrueForPendingTransaction() {
        String txId = "wompi_tx_003";
        String txStatus = "PENDING";
        Long amountInCents = 8000000L;
        Long timestamp = 1715000002L;

        String checksum = computeChecksum(txId, txStatus, amountInCents, timestamp, TEST_EVENTS_KEY);
        PaymentWebhookRequest request = buildRequest(txId, txStatus, amountInCents, timestamp, checksum);

        assertTrue(validator.isValid(request));
    }

    // ─── Checksum inválido ────────────────────────────────────────────────────

    @Test
    void shouldReturnFalseIfChecksumDoesNotMatch() {
        PaymentWebhookRequest request = buildRequest(
                "wompi_tx_001", "APPROVED", 12000000L, 1715000000L, "checksum-invalido"
        );
        assertFalse(validator.isValid(request));
    }

    @Test
    void shouldReturnFalseIfEventsKeyIsWrong() {
        String checksum = computeChecksum("wompi_tx_001", "APPROVED", 12000000L, 1715000000L, "wrong-key");
        PaymentWebhookRequest request = buildRequest(
                "wompi_tx_001", "APPROVED", 12000000L, 1715000000L, checksum
        );
        assertFalse(validator.isValid(request));
    }

    @Test
    void shouldReturnFalseIfTimestampIsWrong() {
        Long realTimestamp = 1715000000L;
        Long tamperedTimestamp = 1715000001L;

        String checksum = computeChecksum("wompi_tx_001", "APPROVED", 12000000L, realTimestamp, TEST_EVENTS_KEY);
        PaymentWebhookRequest request = buildRequest(
                "wompi_tx_001", "APPROVED", 12000000L, tamperedTimestamp, checksum
        );
        assertFalse(validator.isValid(request));
    }

    @Test
    void shouldReturnFalseIfAmountIsWrong() {
        Long realAmount = 12000000L;
        Long tamperedAmount = 1000L;

        String checksum = computeChecksum("wompi_tx_001", "APPROVED", realAmount, 1715000000L, TEST_EVENTS_KEY);
        PaymentWebhookRequest request = buildRequest(
                "wompi_tx_001", "APPROVED", tamperedAmount, 1715000000L, checksum
        );
        assertFalse(validator.isValid(request));
    }

    @Test
    void shouldReturnFalseIfTransactionIdIsWrong() {
        String checksum = computeChecksum("wompi_tx_001", "APPROVED", 12000000L, 1715000000L, TEST_EVENTS_KEY);
        PaymentWebhookRequest request = buildRequest(
                "wompi_tx_TAMPERED", "APPROVED", 12000000L, 1715000000L, checksum
        );
        assertFalse(validator.isValid(request));
    }

    @Test
    void shouldReturnFalseIfStatusIsWrong() {
        String checksum = computeChecksum("wompi_tx_001", "APPROVED", 12000000L, 1715000000L, TEST_EVENTS_KEY);
        PaymentWebhookRequest request = buildRequest(
                "wompi_tx_001", "DECLINED", 12000000L, 1715000000L, checksum
        );
        assertFalse(validator.isValid(request));
    }

    // ─── Propiedades desconocidas ─────────────────────────────────────────────

    @Test
    void shouldReturnFalseForUnknownPropertyEvenIfChecksumWasCorrect() {
        // El checksum fue calculado con las 3 propiedades estándar.
        // Al incluir una propiedad desconocida, resolveProperty aporta ""
        // al hash → el hash resultante no va a coincidir → false.
        String txId = "wompi_tx_001";
        String txStatus = "APPROVED";
        Long amountInCents = 12000000L;
        Long timestamp = 1715000000L;

        String checksum = computeChecksum(txId, txStatus, amountInCents, timestamp, TEST_EVENTS_KEY);

        PaymentWebhookRequest request = buildRequestWithCustomProperties(
                txId, txStatus, amountInCents, timestamp, checksum,
                List.of("transaction.id", "transaction.status", "transaction.unknown")
        );

        assertFalse(validator.isValid(request));
    }

    @Test
    void shouldReturnFalseIfPropertiesListIsEmpty() {
        // Sin propiedades, el raw solo contiene timestamp + eventsKey
        // → el hash no puede coincidir con uno calculado con las props estándar
        String checksum = computeChecksum("wompi_tx_001", "APPROVED", 12000000L, 1715000000L, TEST_EVENTS_KEY);

        PaymentWebhookRequest request = buildRequestWithCustomProperties(
                "wompi_tx_001", "APPROVED", 12000000L, 1715000000L, checksum,
                List.of()
        );

        assertFalse(validator.isValid(request));
    }

    // ─── Nulls — el catch + log.error los captura ─────────────────────────────

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
        PaymentWebhookRequest request = buildRequest("wompi_tx_001", "APPROVED", 12000000L, 1715000000L,
                computeChecksum("wompi_tx_001", "APPROVED", 12000000L, 1715000000L, TEST_EVENTS_KEY));
        request.setSignature(null);

        assertFalse(validator.isValid(request));
    }

    @Test
    void shouldReturnFalseIfRequestItselfIsNull() {
        // NPE en request.getData() → capturado por catch → log.error → false
        assertFalse(validator.isValid(null));
    }

    @Test
    void shouldReturnFalseIfPropertiesListIsNull() {
        PaymentWebhookRequest request = buildRequest("wompi_tx_001", "APPROVED", 12000000L, 1715000000L, "any");
        request.getSignature().setProperties(null);

        assertFalse(validator.isValid(request));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private PaymentWebhookRequest buildRequest(
            String txId, String status, Long amountInCents, Long timestamp, String checksum
    ) {
        return buildRequestWithCustomProperties(
                txId, status, amountInCents, timestamp, checksum,
                List.of("transaction.id", "transaction.status", "transaction.amount_in_cents")
        );
    }

    private PaymentWebhookRequest buildRequestWithCustomProperties(
            String txId, String status, Long amountInCents, Long timestamp, String checksum,
            List<String> properties
    ) {
        PaymentWebhookRequest.Transaction tx = new PaymentWebhookRequest.Transaction();
        tx.setId(txId);
        tx.setStatus(status);
        tx.setAmountInCents(amountInCents);

        PaymentWebhookRequest.SignatureData sig = new PaymentWebhookRequest.SignatureData();
        sig.setChecksum(checksum);
        sig.setProperties(properties);

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