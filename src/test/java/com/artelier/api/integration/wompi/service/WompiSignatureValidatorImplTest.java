package com.artelier.api.integration.wompi.service;

import com.artelier.api.dto.request.PaymentWebhookRequest;
import com.artelier.api.dto.request.PaymentWebhookRequest.SignatureData;
import com.artelier.api.dto.request.PaymentWebhookRequest.Transaction;
import com.artelier.api.dto.request.PaymentWebhookRequest.TransactionData;
import com.artelier.api.integration.wompi.service.impl.WompiSignatureValidatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WompiSignatureValidatorImpl Unit Tests")
class WompiSignatureValidatorImplTest {

    private static final String EVENTS_KEY = "test-events-key";
    private static final String EVENT = "transaction.updated";
    private static final String TRANSACTION_ID = "TX-123456";
    private static final String TRANSACTION_STATUS = "APPROVED";
    private static final long AMOUNT_IN_CENTS = 100000L;
    private static final long TIMESTAMP = 1715000000000L;

    private WompiSignatureValidatorImpl validator;

    @BeforeEach
    void setUp() {
        validator = new WompiSignatureValidatorImpl();

        ReflectionTestUtils.setField(
                validator,
                "eventsKey",
                EVENTS_KEY
        );
    }

    @Nested
    @DisplayName("Valid Signature Scenarios")
    class ValidSignatureTests {

        @Test
        @DisplayName("Should accept a valid signature with all supported properties")
        void shouldAcceptValidSignatureWithAllSupportedProperties() {
            List<String> properties = List.of(
                    "transaction.id",
                    "transaction.status",
                    "transaction.amount_in_cents"
            );

            PaymentWebhookRequest request = createSignedRequest(
                    properties,
                    TIMESTAMP
            );

            assertTrue(validator.isValid(request));
        }

        @Test
        @DisplayName("Should accept a valid signature containing only the transaction ID")
        void shouldAcceptValidSignatureWithTransactionIdOnly() {
            PaymentWebhookRequest request = createSignedRequest(
                    List.of("transaction.id"),
                    TIMESTAMP
            );

            assertTrue(validator.isValid(request));
        }

        @Test
        @DisplayName("Should accept a valid signature containing only the transaction status")
        void shouldAcceptValidSignatureWithTransactionStatusOnly() {
            PaymentWebhookRequest request = createSignedRequest(
                    List.of("transaction.status"),
                    TIMESTAMP
            );

            assertTrue(validator.isValid(request));
        }

        @Test
        @DisplayName("Should accept a valid signature containing only the transaction amount")
        void shouldAcceptValidSignatureWithTransactionAmountOnly() {
            PaymentWebhookRequest request = createSignedRequest(
                    List.of("transaction.amount_in_cents"),
                    TIMESTAMP
            );

            assertTrue(validator.isValid(request));
        }

        @Test
        @DisplayName("Should accept a valid signature with a different property order")
        void shouldAcceptValidSignatureWithDifferentPropertyOrder() {
            List<String> properties = List.of(
                    "transaction.amount_in_cents",
                    "transaction.id",
                    "transaction.status"
            );

            PaymentWebhookRequest request = createSignedRequest(
                    properties,
                    TIMESTAMP
            );

            assertTrue(validator.isValid(request));
        }

        @Test
        @DisplayName("Should accept a valid signature with an empty property list")
        void shouldAcceptValidSignatureWithEmptyPropertyList() {
            PaymentWebhookRequest request = createSignedRequest(
                    List.of(),
                    TIMESTAMP
            );

            assertTrue(validator.isValid(request));
        }
    }

    @Nested
    @DisplayName("Invalid Signature Scenarios")
    class InvalidSignatureTests {

        @Test
        @DisplayName("Should reject an incorrect checksum")
        void shouldRejectIncorrectChecksum() {
            PaymentWebhookRequest request = createRequest(
                    "invalid-checksum",
                    List.of(
                            "transaction.id",
                            "transaction.status",
                            "transaction.amount_in_cents"
                    ),
                    TIMESTAMP
            );

            assertFalse(validator.isValid(request));
        }

        @Test
        @DisplayName("Should reject a signature generated with a different events key")
        void shouldRejectSignatureGeneratedWithDifferentEventsKey() {
            PaymentWebhookRequest request = createSignedRequest(
                    List.of("transaction.id"),
                    TIMESTAMP,
                    "another-events-key"
            );

            assertFalse(validator.isValid(request));
        }

        @Test
        @DisplayName("Should reject a signature generated with a different timestamp")
        void shouldRejectSignatureGeneratedWithDifferentTimestamp() {
            PaymentWebhookRequest request = createSignedRequest(
                    List.of("transaction.id"),
                    TIMESTAMP + 1
            );

            PaymentWebhookRequest tamperedRequest = new PaymentWebhookRequest(
                    request.event(),
                    request.data(),
                    request.signature(),
                    TIMESTAMP
            );

            assertFalse(validator.isValid(tamperedRequest));
        }

        @Test
        @DisplayName("Should reject a signature when a transaction property changes")
        void shouldRejectSignatureWhenTransactionPropertyChanges() {
            PaymentWebhookRequest signedRequest = createSignedRequest(
                    List.of("transaction.id"),
                    TIMESTAMP
            );

            Transaction modifiedTransaction = new Transaction(
                    "TX-MODIFIED",
                    "ORDER-123",
                    TRANSACTION_STATUS,
                    "CARD",
                    AMOUNT_IN_CENTS
            );

            PaymentWebhookRequest tamperedRequest = new PaymentWebhookRequest(
                    EVENT,
                    new TransactionData(modifiedTransaction),
                    signedRequest.signature(),
                    TIMESTAMP
            );

            assertFalse(validator.isValid(tamperedRequest));
        }

        @Test
        @DisplayName("Should reject a signature when the property order changes")
        void shouldRejectSignatureWhenPropertyOrderChanges() {
            List<String> originalProperties = List.of(
                    "transaction.id",
                    "transaction.status"
            );

            PaymentWebhookRequest signedRequest = createSignedRequest(
                    originalProperties,
                    TIMESTAMP
            );

            PaymentWebhookRequest tamperedRequest = new PaymentWebhookRequest(
                    EVENT,
                    signedRequest.data(),
                    new SignatureData(
                            signedRequest.signature().checksum(),
                            List.of(
                                    "transaction.status",
                                    "transaction.id"
                            )
                    ),
                    TIMESTAMP
            );

            assertFalse(validator.isValid(tamperedRequest));
        }

        @Test
        @DisplayName("Should reject a checksum with different letter casing")
        void shouldRejectChecksumWithDifferentLetterCasing() {
            PaymentWebhookRequest signedRequest = createSignedRequest(
                    List.of("transaction.id"),
                    TIMESTAMP
            );

            String checksum = signedRequest.signature().checksum();

            String changedChecksum = checksum.toUpperCase();

            PaymentWebhookRequest tamperedRequest = new PaymentWebhookRequest(
                    EVENT,
                    signedRequest.data(),
                    new SignatureData(
                            changedChecksum,
                            signedRequest.signature().properties()
                    ),
                    TIMESTAMP
            );

            assertFalse(validator.isValid(tamperedRequest));
        }
    }

    @Nested
    @DisplayName("Unsupported Property Scenarios")
    class UnsupportedPropertyTests {

        @Test
        @DisplayName("Should reject a signature containing an unknown property")
        void shouldRejectUnknownProperty() {
            PaymentWebhookRequest request = createSignedRequest(
                    List.of("transaction.unknown"),
                    TIMESTAMP
            );

            assertFalse(validator.isValid(request));
        }

        @Test
        @DisplayName("Should reject a signature containing a blank property")
        void shouldRejectBlankProperty() {
            PaymentWebhookRequest request = createSignedRequest(
                    List.of(""),
                    TIMESTAMP
            );

            assertFalse(validator.isValid(request));
        }

        @Test
        @DisplayName("Should reject a signature containing a null property")
        void shouldRejectNullProperty() {
            PaymentWebhookRequest request = createSignedRequest(
                    java.util.Arrays.asList((String) null),
                    TIMESTAMP
            );

            assertFalse(validator.isValid(request));
        }

        @Test
        @DisplayName("Should reject a signature when an unknown property is mixed with valid properties")
        void shouldRejectUnknownPropertyMixedWithValidProperties() {
            PaymentWebhookRequest request = createSignedRequest(
                    List.of(
                            "transaction.id",
                            "transaction.unknown"
                    ),
                    TIMESTAMP
            );

            assertFalse(validator.isValid(request));
        }
    }

    @Nested
    @DisplayName("Exception Handling")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should return false when the request is null")
        void shouldReturnFalseWhenRequestIsNull() {
            assertFalse(validator.isValid(null));
        }

        @Test
        @DisplayName("Should return false when transaction data is null")
        void shouldReturnFalseWhenTransactionDataIsNull() {
            PaymentWebhookRequest request = new PaymentWebhookRequest(
                    EVENT,
                    null,
                    new SignatureData("checksum", List.of("transaction.id")),
                    TIMESTAMP
            );

            assertFalse(validator.isValid(request));
        }

        @Test
        @DisplayName("Should return false when the transaction is null")
        void shouldReturnFalseWhenTransactionIsNull() {
            PaymentWebhookRequest request = new PaymentWebhookRequest(
                    EVENT,
                    new TransactionData(null),
                    new SignatureData("checksum", List.of("transaction.id")),
                    TIMESTAMP
            );

            assertFalse(validator.isValid(request));
        }

        @Test
        @DisplayName("Should return false when signature data is null")
        void shouldReturnFalseWhenSignatureDataIsNull() {
            PaymentWebhookRequest request = new PaymentWebhookRequest(
                    EVENT,
                    new TransactionData(createTransaction()),
                    null,
                    TIMESTAMP
            );

            assertFalse(validator.isValid(request));
        }

        @Test
        @DisplayName("Should return false when the property list is null")
        void shouldReturnFalseWhenPropertiesAreNull() {
            PaymentWebhookRequest request = new PaymentWebhookRequest(
                    EVENT,
                    new TransactionData(createTransaction()),
                    new SignatureData("checksum", null),
                    TIMESTAMP
            );

            assertFalse(validator.isValid(request));
        }

        @Test
        @DisplayName("Should return false when the timestamp is null")
        void shouldReturnFalseWhenTimestampIsNull() {
            PaymentWebhookRequest request = new PaymentWebhookRequest(
                    EVENT,
                    new TransactionData(createTransaction()),
                    new SignatureData("checksum", List.of("transaction.id")),
                    null
            );

            assertFalse(validator.isValid(request));
        }

        @Test
        @DisplayName("Should return false when the transaction amount is null and included in the signature")
        void shouldReturnFalseWhenTransactionAmountIsNull() {
            Transaction transaction = new Transaction(
                    TRANSACTION_ID,
                    "ORDER-123",
                    TRANSACTION_STATUS,
                    "CARD",
                    null
            );

            PaymentWebhookRequest request = new PaymentWebhookRequest(
                    EVENT,
                    new TransactionData(transaction),
                    new SignatureData(
                            "checksum",
                            List.of("transaction.amount_in_cents")
                    ),
                    TIMESTAMP
            );

            assertFalse(validator.isValid(request));
        }
    }

    private String resolvePropertyForTest(
            PaymentWebhookRequest.Transaction tx,
            String prop
    ) {
        if (prop == null || prop.isBlank()) {
            return "";
        }

        return switch (prop) {
            case "transaction.id" -> tx.id();
            case "transaction.status" -> tx.status();
            case "transaction.amount_in_cents" ->
                    String.valueOf(tx.amountInCents());
            default -> "";
        };
    }

    private PaymentWebhookRequest createSignedRequest(
            List<String> properties,
            Long timestamp
    ) {
        return createSignedRequest(properties, timestamp, EVENTS_KEY);
    }

    private PaymentWebhookRequest createSignedRequest(
            List<String> properties,
            Long timestamp,
            String eventsKey
    ) {
        Transaction transaction = createTransaction();

        String checksum = calculateChecksum(
                transaction,
                properties,
                timestamp,
                eventsKey
        );

        return new PaymentWebhookRequest(
                EVENT,
                new TransactionData(transaction),
                new SignatureData(checksum, properties),
                timestamp
        );
    }

    private PaymentWebhookRequest createRequest(
            String checksum,
            List<String> properties,
            Long timestamp
    ) {
        return new PaymentWebhookRequest(
                EVENT,
                new TransactionData(createTransaction()),
                new SignatureData(checksum, properties),
                timestamp
        );
    }

    private Transaction createTransaction() {
        return new Transaction(
                TRANSACTION_ID,
                "ORDER-123",
                TRANSACTION_STATUS,
                "CARD",
                AMOUNT_IN_CENTS
        );
    }

    private String calculateChecksum(
            Transaction transaction,
            List<String> properties,
            Long timestamp,
            String eventsKey
    ) {
        StringBuilder raw = new StringBuilder();

        for (String property : properties) {
            raw.append(resolvePropertyForTest(transaction, property));
        }

        raw.append(timestamp);
        raw.append(eventsKey);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    raw.toString().getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new AssertionError(
                    "Unable to calculate the expected SHA-256 checksum",
                    exception
            );
        }
    }

    private String resolveProperty(
            Transaction transaction,
            String property
    ) {
        return switch (property) {
            case "transaction.id" -> transaction.id();
            case "transaction.status" -> transaction.status();
            case "transaction.amount_in_cents" ->
                    String.valueOf(transaction.amountInCents());
            default -> "";
        };
    }
}
