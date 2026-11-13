package com.artelier.api.util;

import io.jsonwebtoken.Jwts;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Utility builder class extending {@link JwtCore} to construct and sign JSON Web Tokens (JWT)
 * with custom claims, expiration times, key identifiers, and subjects.
 */
public class JwtTokenBuilder extends JwtCore {

    /**
     * Constructs a new {@code JwtTokenBuilder} with a specified character set,
     * initializing the underlying cryptographic core.
     *
     * @param secret           the secret string used for signing
     * @param charset          the character set used to convert the secret into bytes
     * @param expirationMillis the token expiration time in milliseconds
     * @param issuer           the token issuer identifier
     */
    public JwtTokenBuilder(String secret, Charset charset, long expirationMillis, String issuer) {
        super(secret, charset, expirationMillis, issuer);
    }

    /**
     * Constructs a new {@code JwtTokenBuilder} using standard UTF-8 charset encoding.
     *
     * @param secret           the secret string used for signing
     * @param expirationMillis the token expiration time in milliseconds
     * @param issuer           the token issuer identifier
     */
    public JwtTokenBuilder(String secret, long expirationMillis, String issuer) {
        super(secret, expirationMillis, issuer);
    }

    /**
     * Generates a signed JWT compact string using the specified parameters, claims, and optional key ID.
     *
     * @param secret           the secret string used to derive the signing key
     * @param expirationMillis the token expiration time in milliseconds
     * @param issuer           the expected token issuer
     * @param keyId            the optional key identifier to include in the JWT header (can be null or blank)
     * @param subject          the subject (typically the user ID) of the token
     * @param idToken          the unique identifier (typically the session ID) of the token
     * @param claims           a map of custom claims to include in the token payload (can be null or empty)
     * @return the compact signed JWT string
     */
    public static String generateToken(
            String secret,
            long expirationMillis,
            String issuer,
            String keyId,
            String subject,
            String idToken,
            Map<String, Object> claims
    ) {
        JwtCore validator = new JwtCore(secret, expirationMillis, issuer) {};

        Instant now = Instant.now();

        var builder = Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .id(idToken)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(validator.secretKey);

        if (keyId != null && !keyId.isBlank()) {
            builder.header().keyId(keyId);
        }

        if (claims != null && !claims.isEmpty()) {
            builder.claims(claims);
        }

        return builder.compact();
    }

    /**
     * Generates a signed JWT compact string using the specified parameters, key identifier, subject,
     * and session token identifier, without additional custom claims.
     *
     * @param secret           the secret string used to derive the signing key
     * @param expirationMillis the token expiration time in milliseconds
     * @param issuer           the expected token issuer
     * @param keyId            the optional key identifier to include in the JWT header
     * @param subject          the subject of the token
     * @param idToken          the unique session token identifier
     * @return the compact signed JWT string
     */
    public static String generateToken(
            String secret,
            long expirationMillis,
            String issuer,
            String keyId,
            String subject,
            String idToken
    ) {
        return generateToken(secret, expirationMillis, issuer, keyId, subject, idToken, null);
    }

    /**
     * Generates a signed JWT compact string using the specified parameters, key identifier, and subject,
     * without token identifiers or custom claims.
     *
     * @param secret           the secret string used to derive the signing key
     * @param expirationMillis the token expiration time in milliseconds
     * @param issuer           the expected token issuer
     * @param keyId            the optional key identifier to include in the JWT header
     * @param subject          the subject of the token
     * @return the compact signed JWT string
     */
    public static String generateToken(
            String secret,
            long expirationMillis,
            String issuer,
            String keyId,
            String subject
    ) {
        return generateToken(secret, expirationMillis, issuer, keyId, subject, null, null);
    }

    /**
     * Generates a basic signed JWT compact string using only the secret, expiration, issuer, and subject.
     *
     * @param secret           the secret string used to derive the signing key
     * @param expirationMillis the token expiration time in milliseconds
     * @param issuer           the expected token issuer
     * @param subject          the subject of the token
     * @return the compact signed JWT string
     */
    public static String generateToken(
            String secret,
            long expirationMillis,
            String issuer,
            String subject
    ) {
        return generateToken(secret, expirationMillis, issuer, null, subject, null, null);
    }

    /**
     * Generates a signed JWT compact string using the specified parameters, subject, and custom claims,
     * without a key identifier or token identifier.
     *
     * @param secret           the secret string used to derive the signing key
     * @param expirationMillis the token expiration time in milliseconds
     * @param issuer           the expected token issuer
     * @param subject          the subject of the token
     * @param claims           a map of custom claims to include in the token payload
     * @return the compact signed JWT string
     */
    public static String generateToken(
            String secret,
            long expirationMillis,
            String issuer,
            String subject,
            Map<String, Object> claims
    ) {
        return generateToken(secret, expirationMillis, issuer, null, subject, null, claims);
    }
}