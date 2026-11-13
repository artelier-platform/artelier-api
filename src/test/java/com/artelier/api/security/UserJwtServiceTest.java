package com.artelier.api.security;

import com.artelier.api.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserJwtService")
class UserJwtServiceTest {

    private static final String SECRET =
            "artelier-test-secret-key-must-be-at-least-32-characters-long";
    private static final String OTHER_SECRET =
            "another-test-secret-key-must-be-at-least-32-characters";
    private static final String KEY_ID = "test-key-id";
    private static final long EXPIRATION_MS = 3_600_000L;
    private static final String ISSUER = "artelier-api";

    private UserJwtService userJwtService;
    private UUID userId;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        userJwtService = jwtUtil(SECRET, EXPIRATION_MS);
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static UserJwtService jwtUtil(String secret, long expirationMs) {
        return new UserJwtService(secret, KEY_ID, ISSUER, expirationMs);
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
        String token = userJwtService.generateAccessToken(userId, sessionId, Role.BUYER);

        assertThat(token).isNotBlank();
        assertThat(userJwtService.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("sub is the user id")
    void subjectIsUserId() {
        String token = userJwtService.generateAccessToken(userId, sessionId, Role.BUYER);

        assertThat(claimsOf(token).getSubject()).isEqualTo(userId.toString());
    }

    @Test
    @DisplayName("sid is the session id")
    void sidIsSessionId() {
        String token = userJwtService.generateAccessToken(userId, sessionId, Role.BUYER);

        assertThat(claimsOf(token).get("jti", String.class)).isEqualTo(sessionId.toString());
    }

    @ParameterizedTest(name = "role {0}")
    @ValueSource(strings = {"BUYER", "ADMIN"})
    @DisplayName("role is stored exactly as given (no ROLE_ prefix added)")
    void roleClaim(Role role) {
        String token = userJwtService.generateAccessToken(userId, sessionId, role);

        assertThat(claimsOf(token).get("role", String.class))
                .isEqualTo(role.name());
    }

    @Test
    @DisplayName("expires after the configured lifetime")
    void expirationMatchesConfiguration() {
        String token = userJwtService.generateAccessToken(userId, sessionId, Role.BUYER);

        Claims claims = claimsOf(token);
        long lifetimeMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        // JWT dates have second precision, so allow one second of rounding.
        assertThat(lifetimeMs).isBetween(EXPIRATION_MS - 1000, EXPIRATION_MS + 1000);
    }

    @Test
    @DisplayName("carries nothing else: no username, email, password or phone")
    void carriesOnlyTheExpectedClaims() {
        String token = userJwtService.generateAccessToken(userId, sessionId, Role.BUYER);

        assertThat(claimsOf(token).keySet())
                .containsExactlyInAnyOrder("iss", "sub", "jti", "role", "iat", "exp");
    }

    @Test
    @DisplayName("two sessions of the same user get different tokens")
    void tokensDifferPerSession() {
        String first = userJwtService.generateAccessToken(userId, UUID.randomUUID(), Role.BUYER);
        String second = userJwtService.generateAccessToken(userId, UUID.randomUUID(), Role.BUYER);

        assertThat(first).isNotEqualTo(second);
    }

    // =========================================================
    // extractors
    // =========================================================

    @Test
    @DisplayName("extractors return what was put in the token")
    void extractorsRoundTrip() {
        String token = userJwtService.generateAccessToken(userId, sessionId, Role.ADMIN);

        assertThat(userJwtService.extractUserId(token)).isEqualTo(userId);
        assertThat(userJwtService.extractSessionId(token)).isEqualTo(sessionId);
        assertThat(userJwtService.extractRole(token)).isEqualTo(Role.ADMIN);
    }

    // =========================================================
    // validateToken
    // =========================================================

    @ParameterizedTest(name = "[{0}] is not valid")
    @ValueSource(strings = {"", " ", "invalid.jwt.token", "not-a-jwt", "a.b.c"})
    @DisplayName("malformed tokens are rejected")
    void malformedTokens(String token) {
        assertThat(userJwtService.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("null is rejected, not an exception")
    void nullToken() {
        assertThat(userJwtService.validateToken(null)).isFalse();
    }

    @Test
    @DisplayName("a token signed with another secret is rejected")
    void differentSecret() {
        String token = jwtUtil(OTHER_SECRET, EXPIRATION_MS)
                .generateAccessToken(userId, sessionId, Role.BUYER);

        assertThat(userJwtService.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("an expired token is rejected")
    void expiredToken() {
        String token = jwtUtil(SECRET, -1000L)
                .generateAccessToken(userId, sessionId, Role.BUYER);

        assertThat(userJwtService.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("tampering with the payload (BUYER → ADMIN) breaks the signature")
    void tamperedPayload() {
        String token = userJwtService.generateAccessToken(userId, sessionId, Role.BUYER);
        String[] parts = token.split("\\.");

        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String forgedPayload = payload.replace("BUYER", "ADMIN");
        assertThat(forgedPayload).isNotEqualTo(payload); // the forgery really changed something

        String forged = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(forgedPayload.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];

        assertThat(userJwtService.validateToken(forged)).isFalse();
    }

    @Test
    @DisplayName("an unsigned token (alg none) is rejected")
    void unsignedToken() {
        String unsigned = Jwts.builder()
                .subject(userId.toString())
                .claim("sid", sessionId.toString())
                .claim("role", "ADMIN")
                .compact();

        assertThat(userJwtService.validateToken(unsigned)).isFalse();
    }

    // =========================================================
    // getExpirationMs
    // =========================================================

    @Test
    @DisplayName("getExpirationMs returns the configured lifetime")
    void expirationMsIsConfigured() {
        assertThat(userJwtService.getExpirationMs()).isEqualTo(EXPIRATION_MS);
    }
}