package com.artelier.api.integration.wompi.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class WompiIntegritySignatureUtil {

    @Value("${wompi.integrity-secret}")
    private String integritySecret;

    /**
     * Generates the Wompi integrity signature required to create a payment transaction.
     *
     * <p>The signature is calculated using the SHA-256 hash algorithm over the
     * concatenation of the following values in the exact order:</p>
     *
     * <pre>
     * reference + amountInCents + currency + integritySecret
     * </pre>
     *
     * <p>Example:</p>
     *
     * <pre>
     * SHA256(
     *     "ORDER-abc-123" +
     *     "100000" +
     *     "COP" +
     *     "my_wompi_integrity_secret"
     * )
     * </pre>
     *
     * <p>This signature is sent to Wompi as part of the transaction request
     * to verify that the payment information has not been altered.</p>
     *
     * @param reference
     *        Unique payment reference that identifies the transaction attempt.
     *        It must be the exact same reference value that will be sent to
     *        Wompi when creating the transaction.
     *        <p>Recommended format:</p>
     *        <pre>ORDER-{uuid}-{timestamp}</pre>
     *        <p>Example:</p>
     *        <pre>ORDER-550e8400-e29b-41d4-a716-446655440000-1718730000</pre>
     *
     * @param amountInCents
     *        Transaction amount expressed in the smallest currency unit.
     *        For COP, this value represents Colombian pesos without decimal
     *        separators.
     *        <p>Examples:</p>
     *        <ul>
     *          <li>10,000 COP → 10000</li>
     *          <li>100,000 COP → 100000</li>
     *          <li>1,000,000 COP → 1000000</li>
     *        </ul>
     *        The value must match exactly the amount sent to Wompi.
     *
     * @param currency
     *        ISO 4217 currency code used for the transaction.
     *        For current integrations, the expected value is:
     *        <pre>COP</pre>
     *        The value must match exactly the currency sent to Wompi.
     *
     * @return
     *        A lowercase hexadecimal representation of the SHA-256 hash
     *        generated from the concatenated transaction data and integrity
     *        secret.
     *
     * @throws IllegalStateException
     *         If the SHA-256 algorithm is not available in the current
     *         Java runtime environment.
     */
    public String generate(String reference, Long amountInCents, String currency) {
        if (reference == null || reference.isBlank())
            throw new IllegalArgumentException("The reference cannot be null or empty");
        if (amountInCents == null || amountInCents <= 0)
            throw new IllegalArgumentException("amountInCents must be greater than 0");
        if (currency == null || currency.isBlank())
            throw new IllegalArgumentException("currency cannot be null or empty");

        String raw = reference + amountInCents + currency + integritySecret;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}