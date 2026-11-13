package com.artelier.api.verification.service.impl;

import com.artelier.api.verification.service.EmailVerificationTokens;
import com.artelier.api.auth.security.JwtTokenBuilder;
import com.artelier.api.auth.security.JwtTokenParser;
import io.jsonwebtoken.JwtException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implementation of {@link EmailVerificationTokens} responsible for issuing and parsing
 * secure JSON Web Tokens (JWT) used for verifying user email addresses.
 */
@Service
public class EmailVerificationTokensImpl implements EmailVerificationTokens {

    private final String secret;
    private final String keyId;
    private final String issuer;
    @Getter
    private final long expirationMs;

    private final JwtTokenParser tokenParser;

    /**
     * Constructs a new {@code EmailVerificationTokensImpl} with injected configuration properties
     * and initializes the underlying token parser.
     *
     * @param secret       the secret key used for signing and verifying verification tokens
     * @param keyId        the key identifier included in the JWT header
     * @param issuer       the expected token issuer identifier
     * @param expirationMs the token expiration time in milliseconds
     */
    public EmailVerificationTokensImpl(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.key.id}") String keyId,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        this.secret = secret;
        this.keyId = keyId;
        this.issuer = issuer;
        this.expirationMs = expirationMs;
        this.tokenParser = new JwtTokenParser(secret, expirationMs, issuer);
    }

    /**
     * Issues a signed JWT verification token for a specific email record identifier.
     *
     * @param emailId the unique identifier of the email record
     * @return the compact signed JWT verification token string
     */
    @Override
    public String issue(UUID emailId) {
        return JwtTokenBuilder.generateToken(
                secret,
                expirationMs,
                issuer,
                keyId,
                emailId.toString()
        );
    }

    /**
     * Parses and validates a JWT verification token, extracting the associated email record identifier.
     *
     * @param token the JWT verification token string
     * @return the extracted email record {@link UUID}
     * @throws JwtException if the token is invalid, expired, or contains a malformed subject
     */
    @Override
    public UUID parse(String token) {
        boolean isValid = tokenParser.validateToken(token);
        if (!isValid) {
            throw new JwtException("Invalid or expired email verification token.");
        }

        try {
            return tokenParser.extractSubjectAsUuid(token);
        } catch (IllegalArgumentException e) {
            throw new JwtException("Invalid email ID format in verification token.", e);
        }
    }
}