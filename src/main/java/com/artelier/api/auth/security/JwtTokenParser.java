package com.artelier.api.auth.security;

import io.jsonwebtoken.*;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.UUID;

/**
 * Utility parser class extending {@link JwtCore} to validate JSON Web Tokens (JWT),
 * verify signatures and issuers, and extract specific claims or standard token payloads
 * with type-safe conversion support.
 */
public class JwtTokenParser extends JwtCore {

    /**
     * Constructs a new {@code JwtTokenParser} with a specified character set,
     * initializing the underlying cryptographic core.
     *
     * @param secret           the secret string used for verifying token signatures
     * @param charset          the character set used to convert the secret into bytes
     * @param expirationMillis the token expiration time in milliseconds
     * @param issuer           the expected token issuer identifier
     */
    public JwtTokenParser(String secret, Charset charset, long expirationMillis, String issuer) {
        super(secret, charset, expirationMillis, issuer);
    }

    /**
     * Constructs a new {@code JwtTokenParser} using standard UTF-8 charset encoding.
     *
     * @param secret           the secret string used for verifying token signatures
     * @param expirationMillis the token expiration time in milliseconds
     * @param issuer           the expected token issuer identifier
     */
    public JwtTokenParser(String secret, long expirationMillis, String issuer) {
        super(secret, expirationMillis, issuer);
    }

    /**
     * Validates the JWT signature, issuer, and expiration date.
     *
     * @param token the JWT access token string to validate
     * @return {@code true} if the token is successfully parsed and valid; {@code false} otherwise
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
     * Extracts a specific claim value by its key name from the token payload.
     *
     * @param token the JWT access token
     * @param claim the name of the claim to extract
     * @return the object value associated with the specified claim
     * @throws IllegalArgumentException if the claim name is null or empty
     * @throws JwtException             if the token is invalid, expired, or cannot be parsed
     */
    public Object extractClaim(String token, String claim) {
        if (claim == null || claim.isEmpty()) {
            throw new IllegalArgumentException("Claim cannot be null or empty");
        }
        return extractAllClaims(token).get(claim);
    }

    /**
     * Extracts a specific claim value by its key name and casts it to the requested type.
     *
     * @param token        the JWT access token
     * @param claim        the name of the claim to extract
     * @param requiredType the target class type to cast the claim value into
     * @param <T>          the type parameter
     * @return the converted claim value of the specified type
     * @throws IllegalArgumentException if the claim name is null or empty
     * @throws JwtException             if the token is invalid or casting fails
     */
    public <T> T extractClaim(String token, String claim, Class<T> requiredType) {
        if (claim == null || claim.isEmpty()) {
            throw new IllegalArgumentException("Claim cannot be null or empty");
        }
        return extractAllClaims(token).get(claim, requiredType);
    }

    /**
     * Extracts the subject claim (typically the user ID) from the JWT token.
     *
     * @param token the JWT access token
     * @return the subject string
     * @throws JwtException if the token is invalid or cannot be parsed
     */
    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts the subject claim and parses it directly into a {@link UUID}.
     *
     * @param token the JWT access token
     * @return the subject parsed as a {@link UUID}
     * @throws JwtException if the subject is missing, empty, or not a valid UUID format
     */
    public UUID extractSubjectAsUuid(String token) {
        String subject = extractSubject(token);
        if (subject == null || subject.isEmpty()) {
            throw new JwtException("Token does not contain a valid subject.");
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new JwtException("Invalid UUID format in token subject: %s".formatted(subject), e);
        }
    }

    /**
     * Extracts the unique token identifier claim (typically the session ID) from the JWT token.
     *
     * @param token the JWT access token
     * @return the token identifier string
     * @throws JwtException if the token is invalid or cannot be parsed
     */
    public String extractId(String token) {
        return extractAllClaims(token).getId();
    }

    /**
     * Extracts the unique token identifier claim and parses it directly into a {@link UUID}.
     *
     * @param token the JWT access token
     * @return the token ID parsed as a {@link UUID}
     * @throws JwtException if the ID claim is missing, empty, or not a valid UUID format
     */
    public UUID extractIdAsUuid(String token) {
        String id = extractId(token);
        if (id == null || id.isEmpty()) {
            throw new JwtException("Token does not contain a valid ID claim.");
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new JwtException("Invalid UUID format in token ID claim: %s".formatted(id), e);
        }
    }

    /**
     * Extracts the issuance timestamp claim from the JWT token.
     *
     * @param token the JWT access token
     * @return the issuance {@link Date}
     * @throws JwtException if the token is invalid or cannot be parsed
     */
    public Date extractIssuedAt(String token) {
        return extractAllClaims(token).getIssuedAt();
    }

    /**
     * Extracts the expiration timestamp claim from the JWT token.
     *
     * @param token the JWT access token
     * @return the expiration {@link Date}
     * @throws JwtException if the token is invalid or cannot be parsed
     */
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    /**
     * Checks whether the JWT token has expired.
     *
     * @param token the JWT access token
     * @return {@code true} if the token expiration date is before the current time; {@code false} otherwise
     * @throws JwtException if the token is invalid or cannot be parsed
     */
    public boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        return expiration != null && expiration.before(new Date());
    }

    /**
     * Parses the JWT and retrieves all verified claims from its payload.
     *
     * @param token the JWT access token
     * @return the validated {@link Claims} payload
     * @throws JwtException if the token is invalid, expired, or has an unexpected issuer
     */
    public Claims extractAllClaims(String token) {
        return getParser()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Creates a JWT parser configured with the cryptographic signing key and expected issuer.
     *
     * @return the configured {@link JwtParser} instance
     */
    private JwtParser getParser() {
        return Jwts.parser()
                .verifyWith(this.secretKey)
                .requireIssuer(issuer)
                .build();
    }
}