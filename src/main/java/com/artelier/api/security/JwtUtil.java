
package com.artelier.api.security;
import com.artelier.api.enums.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;


/**
 * Utility component responsible for generating, validating, and extracting
 * information from JWT access tokens.
 * Uses HMAC-based signing to ensure token integrity and authenticity.
 * Token claims include the user ID, session ID, role, issuer,
 * issuance date, and expiration date.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.key.id}")
    private String keyId;

    @Value("${jwt.issuer}")
    private String issuer;

    @Getter
    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    /**
     * Creates a cryptographic signing key from the configured secret.
     *
     * @return the HMAC signing key
     * @throws IllegalArgumentException if the secret contains fewer than 32 bytes
     */
    private SecretKey getKey() {
        byte[] keyBytes = secret.getBytes(UTF_8);
        int keyLength = keyBytes.length;

        if (keyLength < 32) {
            throw new IllegalArgumentException(
                    "The secret key must be at least 256 bits (32 bytes) long."
            );
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a signed JWT access token for the specified user and session.
     *
     * @param userId the unique identifier of the authenticated user
     * @param sessionId the unique identifier of the associated session
     * @param role the role assigned to the user
     * @return the compact signed JWT access token
     */
    public String generateAccessToken(UUID userId, UUID sessionId, Role role) {
        Instant now = Instant.now();

        return Jwts.builder()
                .header().add(buildHeader()).and()
                .issuer(issuer)
                .subject(userId.toString())
                .id(sessionId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .claim("role", role)
                .signWith(getKey())
                .compact();
    }

    /**
     * Extracts and converts the user ID from the JWT subject claim.
     *
     * @param token the JWT access token
     * @return the user ID as a UUID
     * @throws JwtException if the token is invalid or does not contain a valid user ID
     */
    public UUID extractUserId(String token) {
        Claims claims = extractAllClaims(token);

        String subject = claims.getSubject();

        if (subject == null || subject.isEmpty()) {
            throw new JwtException("Token does not contain a valid user ID.");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new JwtException(
                    "Invalid user ID in token: %s".formatted(subject),
                    e
            );
        }
    }

    /**
     * Extracts and converts the session ID from the JWT ID claim.
     *
     * @param token the JWT access token
     * @return the session ID as a UUID
     * @throws JwtException if the token is invalid or does not contain a valid session ID
     */
    public UUID extractSessionId(String token) {
        Claims claims = extractAllClaims(token);

        String id = claims.getId();

        if (id == null || id.isEmpty()) {
            throw new JwtException("Token does not contain a valid session ID.");
        }

        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new JwtException(
                    "Invalid session ID in token: %s".formatted(id),
                    e
            );
        }
    }

    /**
     * Extracts the user's role from the custom JWT role claim.
     *
     * @param token the JWT access token
     * @return the role claim as an enum
     * @throws JwtException if the token cannot be parsed, validated, or if the role claim is missing/blank/invalid
     */
    public Role extractRole(String token) {
        String roleStr = extractAllClaims(token).get("role", String.class);

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
     * Validates the JWT signature, issuer, and expiration date.
     *
     * @param token the JWT access token to validate
     * @return true if the token is valid; false otherwise
     */
    public boolean validateToken(String token) {
        try {
            getParser()
                    .parseSignedClaims(token);

            return true;

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Builds the JWT header containing the configured key identifier.
     *
     * @return the configured JWT header
     */
    private Header buildHeader() {
        return Jwts.header()
                .keyId(keyId)
                .build();
    }

    /**
     * Parses the JWT and retrieves its claims after validating
     * its signature, issuer, and expiration date.
     *
     * @param token the JWT access token
     * @return the validated JWT claims
     * @throws JwtException if the token is invalid, expired, or has an unexpected issuer
     */
    private Claims extractAllClaims(String token) {
        return getParser()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Creates a JWT parser configured with the signing key and expected issuer.
     *
     * @return the configured JWT parser
     */
    private JwtParser getParser() {
        return Jwts.parser()
                .verifyWith(getKey())
                .requireIssuer(issuer)
                .build();
    }
}
