package com.artelier.api.auth.security;

import com.artelier.api.user.enums.Role;
import com.artelier.api.auth.security.JwtTokenBuilder;
import com.artelier.api.auth.security.JwtTokenParser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Service responsible for the generation, validation, and data extraction
 * for user and session specific JWT tokens within the Artelier application.
 */
@Service
public class UserJwtService {

    private final String secret;
    private final String keyId;
    private final String issuer;
    @Getter
    private final long expirationMs;

    private final JwtTokenParser tokenParser;

    /**
     * Constructs a new {@code UserJwtService} with injected configuration properties
     * and initializes the underlying token parser.
     *
     * @param secret       the secret key used for signing and verifying tokens
     * @param keyId        the key identifier included in the JWT header
     * @param issuer       the expected token issuer identifier
     * @param expirationMs the token expiration time in milliseconds
     */
    public UserJwtService(
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
     * Generates a signed JWT access token for a specific user and session.
     *
     * @param userId    the unique identifier of the user
     * @param sessionId the unique identifier of the active user session
     * @param role      the user's assigned security role
     * @return the compact signed JWT access token string
     */
    public String generateAccessToken(UUID userId, UUID sessionId, Role role) {
        return JwtTokenBuilder.generateToken(
                secret,
                expirationMs,
                issuer,
                keyId,
                userId.toString(),
                sessionId.toString(),
                Map.of("role", role.name())
        );
    }

    /**
     * Extracts and validates the user identifier from the JWT subject.
     *
     * @param token the JWT token string
     * @return the extracted user {@link UUID}
     * @throws JwtException if the subject claim is missing, empty, or represents an invalid UUID format
     */
    public UUID extractUserId(String token) {
        return tokenParser.extractSubjectAsUuid(token);
    }

    /**
     * Extracts and validates the session identifier from the JWT id claim.
     *
     * @param token the JWT token string
     * @return the extracted session {@link UUID}
     * @throws JwtException if the token ID claim is missing, empty, or represents an invalid UUID format
     */
    public UUID extractSessionId(String token) {
        return tokenParser.extractIdAsUuid(token);
    }

    /**
     * Extracts the user role from custom JWT claims.
     *
     * @param token the JWT token string
     * @return the parsed {@link Role} enum value
     * @throws JwtException if the role claim is missing, empty, or does not match a valid role enum
     */
    public Role extractRole(String token) {
        String roleStr = tokenParser.extractClaim(token, "role", String.class);

        if (roleStr == null || roleStr.trim().isEmpty()) {
            throw new JwtException("Role claim is missing or empty in the JWT token");
        }

        try {
            return Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new JwtException("The role '" + roleStr + "' is not a valid enum value", e);
        }
    }

    /**
     * Validates the cryptographic signature, expiration date, and issuer of the token.
     *
     * @param token the JWT token string to validate
     * @return {@code true} if the token is valid; {@code false} otherwise
     */
    public boolean validateToken(String token) {
        return tokenParser.validateToken(token);
    }
}