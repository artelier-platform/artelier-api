package com.artelier.api.integration.wompi.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class WompiIntegritySignatureUtilTest {

    private WompiIntegritySignatureUtil util;

    private static final String TEST_INTEGRITY_SECRET = "test-integrity-secret";

    @BeforeEach
    void setUp() {
        util = new WompiIntegritySignatureUtil();
        ReflectionTestUtils.setField(util, "integritySecret", TEST_INTEGRITY_SECRET);
    }

    // ─── Happy path ───────────────────────────────────────────────────────────

    @Test
    void shouldGenerateExpectedSha256Hash() {
        String reference = "ORDER-550e8400-e29b-41d4-a716-446655440000-1718730000";
        Long amountInCents = 100000L;
        String currency = "COP";

        String expected = computeExpected(reference, amountInCents, currency);
        String result = util.generate(reference, amountInCents, currency);

        assertEquals(expected, result);
    }

    @Test
    void shouldReturnLowercaseHex() {
        String result = util.generate("ORDER-abc-123", 50000L, "COP");

        assertTrue(result.matches("[0-9a-f]{64}"),
                "El hash debe ser hex lowercase de 64 caracteres (SHA-256)");
    }

    @Test
    void shouldProduceDifferentHashesForDifferentReferences() {
        String hash1 = util.generate("ORDER-001", 100000L, "COP");
        String hash2 = util.generate("ORDER-002", 100000L, "COP");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void shouldProduceDifferentHashesForDifferentAmounts() {
        String hash1 = util.generate("ORDER-abc-123", 100000L, "COP");
        String hash2 = util.generate("ORDER-abc-123", 200000L, "COP");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void shouldProduceDifferentHashesForDifferentCurrencies() {
        String hash1 = util.generate("ORDER-abc-123", 100000L, "COP");
        String hash2 = util.generate("ORDER-abc-123", 100000L, "USD");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void shouldProduceDifferentHashWhenSecretChanges() {
        String hashWithTestSecret = util.generate("ORDER-abc-123", 100000L, "COP");

        ReflectionTestUtils.setField(util, "integritySecret", "otro-secret");
        String hashWithOtherSecret = util.generate("ORDER-abc-123", 100000L, "COP");

        assertNotEquals(hashWithTestSecret, hashWithOtherSecret);
    }

    @Test
    void shouldRespectConcatenationOrder() {
        String reference = "REF";
        Long amount = 1000L;
        String currency = "COP";
        String correctOrder = computeExpected(reference, amount, currency);
        String wrongOrder = computeRaw(amount + reference + currency + TEST_INTEGRITY_SECRET);
        String result = util.generate(reference, amount, currency);
        assertEquals(correctOrder, result);
        assertNotEquals(wrongOrder, result);
    }

    @Test
    void shouldThrowWhenReferenceIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> util.generate(null, 100000L, "COP"));

        assertEquals("The reference cannot be null or empty", ex.getMessage());
    }

    @Test
    void shouldThrowWhenReferenceIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> util.generate("   ", 100000L, "COP"));
    }

    @Test
    void shouldThrowWhenAmountIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> util.generate("ORDER-abc-123", null, "COP"));

        assertEquals("amountInCents must be greater than 0", ex.getMessage());
    }

    @Test
    void shouldThrowWhenAmountIsZero() {
        assertThrows(IllegalArgumentException.class,
                () -> util.generate("ORDER-abc-123", 0L, "COP"));
    }

    @Test
    void shouldThrowWhenAmountIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> util.generate("ORDER-abc-123", -1L, "COP"));
    }

    @Test
    void shouldThrowWhenCurrencyIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> util.generate("ORDER-abc-123", 100000L, null));

        assertEquals("currency cannot be null or empty", ex.getMessage());
    }

    @Test
    void shouldThrowWhenCurrencyIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> util.generate("ORDER-abc-123", 100000L, "   "));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String computeExpected(String reference, Long amountInCents, String currency) {
        return computeRaw(reference + amountInCents + currency + WompiIntegritySignatureUtilTest.TEST_INTEGRITY_SECRET);
    }

    private String computeRaw(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}