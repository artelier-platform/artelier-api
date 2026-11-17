package com.artelier.api.shared.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tokens are checked with jjwt directly, not with JwtTokenParser,
 * so a bug in one class cannot hide a bug in the other.
 */
@DisplayName("JwtTokenBuilder")
class JwtTokenBuilderTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-testing-purposes-123456";
    // A multiple of 1000: JWT dates have second precision.
    private static final long EXPIRATION = 900_000L;
    private static final String ISSUER = "https://api.artelier.test";
    private static final String KEY_ID = "key-1";
    private static final String SUBJECT = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TOKEN_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private static Jws<Claims> parse(String token) {
        return Jwts.parser().verifyWith(KEY).requireIssuer(ISSUER).build().parseSignedClaims(token);
    }

    @Nested
    @DisplayName("full overload")
    class FullOverload {

        @Test
        @DisplayName("fills issuer, subject, id and key id")
        void standardFields() {
            String token = JwtTokenBuilder.generateToken(
                    SECRET, EXPIRATION, ISSUER, KEY_ID, SUBJECT, TOKEN_ID, null);

            Jws<Claims> jws = parse(token);

            assertThat(jws.getPayload().getIssuer()).isEqualTo(ISSUER);
            assertThat(jws.getPayload().getSubject()).isEqualTo(SUBJECT);
            assertThat(jws.getPayload().getId()).isEqualTo(TOKEN_ID);
            assertThat(jws.getHeader().getKeyId()).isEqualTo(KEY_ID);
        }

        @Test
        @DisplayName("is signed with HS256 for a 32..47 byte secret")
        void signedWithHs256() {
            String token = JwtTokenBuilder.generateToken(
                    "a".repeat(32), EXPIRATION, ISSUER, KEY_ID, SUBJECT, TOKEN_ID, null);

            SecretKey key = Keys.hmacShaKeyFor("a".repeat(32).getBytes(StandardCharsets.UTF_8));
            Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);

            assertThat(jws.getHeader().getAlgorithm()).isEqualTo("HS256");
        }

        @Test
        @DisplayName("issuedAt is now and expiration is issuedAt + expirationMillis")
        void timestamps() {
            Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);

            String token = JwtTokenBuilder.generateToken(
                    SECRET, EXPIRATION, ISSUER, KEY_ID, SUBJECT, TOKEN_ID, null);

            Instant after = Instant.now();
            Claims claims = parse(token).getPayload();
            assertThat(claims.getIssuedAt().toInstant()).isBetween(before, after);
            assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                    .isEqualTo(EXPIRATION);
        }

        @Test
        @DisplayName("custom claims go into the payload")
        void customClaims() {
            String token = JwtTokenBuilder.generateToken(
                    SECRET, EXPIRATION, ISSUER, KEY_ID, SUBJECT, TOKEN_ID,
                    Map.of("role", "ADMIN", "tokenVersion", 3));

            Claims claims = parse(token).getPayload();

            assertThat(claims.get("role")).isEqualTo("ADMIN");
            assertThat(claims.get("tokenVersion")).isEqualTo(3);
            assertThat(claims.getSubject()).isEqualTo(SUBJECT);
        }

        @Test
        @DisplayName("an empty claims map adds nothing extra")
        void emptyClaims() {
            String token = JwtTokenBuilder.generateToken(
                    SECRET, EXPIRATION, ISSUER, KEY_ID, SUBJECT, TOKEN_ID, Map.of());

            assertThat(parse(token).getPayload().keySet())
                    .containsExactlyInAnyOrder("iss", "sub", "jti", "iat", "exp");
        }

        @ParameterizedTest(name = "keyId=[{0}] -> no kid header")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @DisplayName("a null, empty or blank key id adds no kid header")
        void blankKeyId(String keyId) {
            String token = JwtTokenBuilder.generateToken(SECRET, EXPIRATION, ISSUER, keyId, SUBJECT);

            assertThat(parse(token).getHeader().getKeyId()).isNull();
        }

        @Test
        @DisplayName("a secret shorter than 32 bytes is rejected")
        void shortSecret() {
            assertThatThrownBy(() -> JwtTokenBuilder.generateToken(
                    "too-short", EXPIRATION, ISSUER, KEY_ID, SUBJECT, TOKEN_ID, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("256 bits");
        }
    }

    @Nested
    @DisplayName("convenience overloads")
    class Overloads {

        @Test
        @DisplayName("(secret, exp, issuer, keyId, subject, tokenId): no custom claims")
        void withTokenId() {
            Claims claims = parse(JwtTokenBuilder.generateToken(
                    SECRET, EXPIRATION, ISSUER, KEY_ID, SUBJECT, TOKEN_ID)).getPayload();

            assertThat(claims.getSubject()).isEqualTo(SUBJECT);
            assertThat(claims.getId()).isEqualTo(TOKEN_ID);
        }

        @Test
        @DisplayName("(secret, exp, issuer, keyId, subject): kid and subject, no jti")
        void withKeyId() {
            Jws<Claims> jws = parse(JwtTokenBuilder.generateToken(
                    SECRET, EXPIRATION, ISSUER, KEY_ID, SUBJECT));

            assertThat(jws.getHeader().getKeyId()).isEqualTo(KEY_ID);
            assertThat(jws.getPayload().getSubject()).isEqualTo(SUBJECT);
            assertThat(jws.getPayload().getId()).isNull();
        }

        @Test
        @DisplayName("(secret, exp, issuer, subject): no kid and no jti")
        void minimal() {
            Jws<Claims> jws = parse(JwtTokenBuilder.generateToken(
                    SECRET, EXPIRATION, ISSUER, SUBJECT));

            assertThat(jws.getHeader().getKeyId()).isNull();
            assertThat(jws.getPayload().getId()).isNull();
            assertThat(jws.getPayload().getSubject()).isEqualTo(SUBJECT);
        }

        @Test
        @DisplayName("(secret, exp, issuer, subject, claims): custom claims, no kid and no jti")
        void withClaims() {
            Jws<Claims> jws = parse(JwtTokenBuilder.generateToken(
                    SECRET, EXPIRATION, ISSUER, SUBJECT, Map.of("role", "BUYER")));

            assertThat(jws.getPayload().get("role")).isEqualTo("BUYER");
            assertThat(jws.getHeader().getKeyId()).isNull();
            assertThat(jws.getPayload().getId()).isNull();
        }
    }

    @Nested
    @DisplayName("instance constructors")
    class Constructors {

        @Test
        @DisplayName("accept a valid secret, with or without an explicit charset")
        void valid() {
            assertThat(new JwtTokenBuilder(SECRET, EXPIRATION, ISSUER).getExpirationMillis())
                    .isEqualTo(EXPIRATION);
            assertThat(new JwtTokenBuilder(SECRET, StandardCharsets.UTF_8, EXPIRATION, ISSUER)
                    .getExpirationMillis())
                    .isEqualTo(EXPIRATION);
        }

        @Test
        @DisplayName("reject a secret shorter than 32 bytes")
        void shortSecret() {
            assertThatThrownBy(() -> new JwtTokenBuilder("too-short", EXPIRATION, ISSUER))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() ->
                    new JwtTokenBuilder("too-short", StandardCharsets.UTF_8, EXPIRATION, ISSUER))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
