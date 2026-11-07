package com.artelier.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtUtil")
class JwtUtilTest {

    private static final String SECRET =
            "artelier-test-secret-key-must-be-at-least-32-characters-long";
    private static final String OTHER_SECRET =
            "another-test-secret-key-must-be-at-least-32-characters";
    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtUtil jwtUtil;
    private UUID userId;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        jwtUtil = jwtUtil(SECRET, EXPIRATION_MS);
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static JwtUtil jwtUtil(String secret, long expirationMs) {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", secret);
        ReflectionTestUtils.setField(util, "expirationMs", expirationMs);
        return util;
    }

    /** Reads the token independently of JwtUtil, so the wire format is what is tested. */
    private static Claims claimsOf(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    // =========================================================
    // generateAccessToken: wire format
    // =========================================================

    @Test
    @DisplayName("generates a token that validates")
    void generatesValidToken() {
        String token = jwtUtil.generateAccessToken(userId, sessionId, "BUYER");

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("sub is the user id")
    void subjectIsUserId() {
        String token = jwtUtil.generateAccessToken(userId, sessionId, "BUYER");

        assertThat(claimsOf(token).getSubject()).isEqualTo(userId.toString());
    }

    @Test
    @DisplayName("sid is the session id")
    void sidIsSessionId() {
        String token = jwtUtil.generateAccessToken(userId, sessionId, "BUYER");

        assertThat(claimsOf(token).get("sid", String.class)).isEqualTo(sessionId.toString());
    }

    @ParameterizedTest(name = "role {0}")
    @ValueSource(strings = {"BUYER", "ADMIN"})
    @DisplayName("role is stored exactly as given (no ROLE_ prefix added)")
    void roleClaim(String role) {
        String token = jwtUtil.generateAccessToken(userId, sessionId, role);

        assertThat(claimsOf(token).get("role", String.class)).isEqualTo(role);
    }

    @Test
    @DisplayName("expires after the configured lifetime")
    void expirationMatchesConfiguration() {
        String token = jwtUtil.generateAccessToken(userId, sessionId, "BUYER");

        Claims claims = claimsOf(token);
        long lifetimeMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        // JWT dates have second precision, so allow one second of rounding.
        assertThat(lifetimeMs).isBetween(EXPIRATION_MS - 1000, EXPIRATION_MS + 1000);
    }

    @Test
    @DisplayName("carries nothing else: no username, email, password or phone")
    void carriesOnlyTheExpectedClaims() {
        String token = jwtUtil.generateAccessToken(userId, sessionId, "BUYER");

        assertThat(claimsOf(token).keySet())
                .containsExactlyInAnyOrder("sub", "sid", "role", "iat", "exp");
    }

    @Test
    @DisplayName("two sessions of the same user get different tokens")
    void tokensDifferPerSession() {
        String first = jwtUtil.generateAccessToken(userId, UUID.randomUUID(), "BUYER");
        String second = jwtUtil.generateAccessToken(userId, UUID.randomUUID(), "BUYER");

        assertThat(first).isNotEqualTo(second);
    }

    // =========================================================
    // extractors
    // =========================================================

    @Test
    @DisplayName("extractors return what was put in the token")
    void extractorsRoundTrip() {
        String token = jwtUtil.generateAccessToken(userId, sessionId, "ADMIN");

        assertThat(jwtUtil.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtUtil.extractSessionId(token)).isEqualTo(sessionId);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    // =========================================================
    // validateToken
    // =========================================================

    @ParameterizedTest(name = "[{0}] is not valid")
    @ValueSource(strings = {"", " ", "invalid.jwt.token", "not-a-jwt", "a.b.c"})
    @DisplayName("malformed tokens are rejected")
    void malformedTokens(String token) {
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("null is rejected, not an exception")
    void nullToken() {
        assertThat(jwtUtil.validateToken(null)).isFalse();
    }

    @Test
    @DisplayName("a token signed with another secret is rejected")
    void differentSecret() {
        String token = jwtUtil(OTHER_SECRET, EXPIRATION_MS)
                .generateAccessToken(userId, sessionId, "BUYER");

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("an expired token is rejected")
    void expiredToken() {
        String token = jwtUtil(SECRET, -1000L)
                .generateAccessToken(userId, sessionId, "BUYER");

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("tampering with the payload (BUYER → ADMIN) breaks the signature")
    void tamperedPayload() {
        String token = jwtUtil.generateAccessToken(userId, sessionId, "BUYER");
        String[] parts = token.split("\\.");

        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String forgedPayload = payload.replace("BUYER", "ADMIN");
        assertThat(forgedPayload).isNotEqualTo(payload); // the forgery really changed something

        String forged = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(forgedPayload.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];

        assertThat(jwtUtil.validateToken(forged)).isFalse();
    }

    @Test
    @DisplayName("an unsigned token (alg none) is rejected")
    void unsignedToken() {
        String unsigned = Jwts.builder()
                .subject(userId.toString())
                .claim("sid", sessionId.toString())
                .claim("role", "ADMIN")
                .compact();

        assertThat(jwtUtil.validateToken(unsigned)).isFalse();
    }

    // =========================================================
    // getExpirationMs
    // =========================================================

    @Test
    @DisplayName("getExpirationMs returns the configured lifetime")
    void expirationMsIsConfigured() {
        assertThat(jwtUtil.getExpirationMs()).isEqualTo(EXPIRATION_MS);
    }
}