package com.artelier.api.auth.security;

import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;

/**
 * Abstract base class providing core cryptographic initialization, secret key generation,
 * and configuration properties for JWT utility components.
 *
 * <p>Handles secret validation, minimum key length requirements (at least 256 bits / 32 bytes),
 * and HMAC signing key derivation using Lombok's {@link SuperBuilder}.</p>
 */
@SuperBuilder
public abstract class JwtCore {

    /**
     * The raw secret string used for HMAC signing and validation.
     */
    protected final String secret;

    /**
     * The token expiration time in milliseconds.
     */
    @Getter
    protected final long expirationMillis;

    /**
     * The expected issuer identifier for generated and parsed tokens.
     */
    protected final String issuer;

    /**
     * The cryptographic secret key derived from the secret string.
     */
    protected final SecretKey secretKey;

    /**
     * Constructs a new {@code JwtCore} instance with a specified character set,
     * validating the key length and initializing the cryptographic signing key.
     *
     * @param secret           the secret string used for signing
     * @param charset          the character set used to convert the secret into bytes (defaults to UTF-8 if null)
     * @param expirationMillis the token expiration time in milliseconds
     * @param issuer           the token issuer identifier
     * @throws IllegalArgumentException if the secret key contains fewer than 32 bytes
     */
    protected JwtCore(String secret, Charset charset, long expirationMillis, String issuer) {
        this.secret = secret;
        this.expirationMillis = expirationMillis;
        this.issuer = issuer;

        Charset effectiveCharset = (charset != null) ? charset : StandardCharsets.UTF_8;

        byte[] keyBytes = secret.getBytes(effectiveCharset);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "The secret key must be at least 256 bits (32 bytes) long."
            );
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Constructs a new {@code JwtCore} instance using standard UTF-8 charset encoding.
     *
     * @param secret           the secret string used for signing
     * @param expirationMillis the token expiration time in milliseconds
     * @param issuer           the token issuer identifier
     * @throws IllegalArgumentException if the secret key contains fewer than 32 bytes
     */
    protected JwtCore(String secret, long expirationMillis, String issuer) {
        this(secret, StandardCharsets.UTF_8, expirationMillis, issuer);
    }
}