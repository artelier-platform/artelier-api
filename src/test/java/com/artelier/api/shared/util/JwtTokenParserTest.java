package com.artelier.api.shared.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.MissingClaimException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tokens are built with JwtTokenBuilder (the real production path) and, for the
 * hostile cases (unsigned, forged, no issuer), directly with jjwt.
 */
@DisplayName("JwtTokenParser")
class JwtTokenParserTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-testing-purposes-123456";
    private static final String OTHER_SECRET = "another-secret-key-that-is-also-long-enough-for-testing-9876";
    private static final long EXPIRATION = 900_000L;
    private static final String ISSUER = "https://api.artelier.test";
    private static final UUID SUBJECT = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID TOKEN_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

    private JwtTokenParser parser;
    private String validToken;

    @BeforeEach
    void setUp() {
        parser = new JwtTokenParser(SECRET, EXPIRATION, ISSUER);
        validToken = JwtTokenBuilder.generateToken(
                SECRET, EXPIRATION, ISSUER, "key-1", SUBJECT.toString(), TOKEN_ID.toString(),
                Map.of("role", "ADMIN", "tokenVersion", 3));
    }

    private static String expiredToken() {
        return JwtTokenBuilder.generateToken(SECRET, -60_000L, ISSUER, SUBJECT.toString());
    }

    private static String tokenWithoutSubject() {
        return JwtTokenBuilder.generateToken(SECRET, EXPIRATION, ISSUER, null);
    }

    private static String tokenWithSubject(String subject) {
        return JwtTokenBuilder.generateToken(SECRET, EXPIRATION, ISSUER, subject);
    }

    private static String tokenWithId(String id) {
        return JwtTokenBuilder.generateToken(SECRET, EXPIRATION, ISSUER, null, SUBJECT.toString(), id);
    }

    private static String signedTokenWithoutIssuer() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(SUBJECT.toString())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key)
                .compact();
    }

    private static String unsignedToken() {
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(SUBJECT.toString())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .compact();
    }

    /** Valid header and signature, but a swapped payload: the signature no longer matches. */
    private String forgedToken() {
        String[] parts = validToken.split("\\.");
        String payload = "{\"iss\":\"" + ISSUER + "\",\"sub\":\"" + UUID.randomUUID()
                + "\",\"exp\":4102444800}";
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return parts[0] + "." + forgedPayload + "." + parts[2];
    }

    // =========================================================
    // validateToken
    // =========================================================

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("true for a good token")
        void valid() {
            assertThat(parser.validateToken(validToken)).isTrue();
        }

        @Test
        @DisplayName("false when signed with another secret")
        void wrongSecret() {
            String token = JwtTokenBuilder.generateToken(
                    OTHER_SECRET, EXPIRATION, ISSUER, SUBJECT.toString());

            assertThat(parser.validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("false for another issuer")
        void wrongIssuer() {
            String token = JwtTokenBuilder.generateToken(
                    SECRET, EXPIRATION, "https://evil.test", SUBJECT.toString());

            assertThat(parser.validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("false when there is no issuer claim")
        void missingIssuer() {
            assertThat(parser.validateToken(signedTokenWithoutIssuer())).isFalse();
        }

        @Test
        @DisplayName("false for an expired token")
        void expired() {
            assertThat(parser.validateToken(expiredToken())).isFalse();
        }

        @Test
        @DisplayName("false when the payload was altered after signing")
        void forged() {
            assertThat(parser.validateToken(forgedToken())).isFalse();
        }

        @Test
        @DisplayName("false for an unsigned token (alg=none)")
        void unsigned() {
            assertThat(parser.validateToken(unsignedToken())).isFalse();
        }

        @ParameterizedTest(name = "[{0}] -> false, never an exception")
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "garbage", "a.b.c", "a.b.c.d"})
        @DisplayName("false for null, empty or malformed input")
        void garbage(String token) {
            assertThat(parser.validateToken(token)).isFalse();
        }
    }

    // =========================================================
    // extractAllClaims
    // =========================================================

    @Nested
    @DisplayName("extractAllClaims")
    class ExtractAllClaims {

        @Test
        @DisplayName("returns the verified payload")
        void payload() {
            assertThat(parser.extractAllClaims(validToken).getIssuer()).isEqualTo(ISSUER);
            assertThat(parser.extractAllClaims(validToken).getSubject()).isEqualTo(SUBJECT.toString());
        }

        @Test
        @DisplayName("wrong secret: SignatureException")
        void wrongSecret() {
            JwtTokenParser other = new JwtTokenParser(OTHER_SECRET, EXPIRATION, ISSUER);

            assertThatThrownBy(() -> other.extractAllClaims(validToken))
                    .isInstanceOf(SignatureException.class);
        }

        @Test
        @DisplayName("another issuer: IncorrectClaimException")
        void wrongIssuer() {
            JwtTokenParser other = new JwtTokenParser(SECRET, EXPIRATION, "https://evil.test");

            assertThatThrownBy(() -> other.extractAllClaims(validToken))
                    .isInstanceOf(IncorrectClaimException.class);
        }

        @Test
        @DisplayName("no issuer claim: MissingClaimException")
        void missingIssuer() {
            assertThatThrownBy(() -> parser.extractAllClaims(signedTokenWithoutIssuer()))
                    .isInstanceOf(MissingClaimException.class);
        }

        @Test
        @DisplayName("expired: ExpiredJwtException")
        void expired() {
            assertThatThrownBy(() -> parser.extractAllClaims(expiredToken()))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("malformed: MalformedJwtException")
        void malformed() {
            assertThatThrownBy(() -> parser.extractAllClaims("garbage"))
                    .isInstanceOf(MalformedJwtException.class);
        }

        @Test
        @DisplayName("forged payload: SignatureException")
        void forged() {
            assertThatThrownBy(() -> parser.extractAllClaims(forgedToken()))
                    .isInstanceOf(SignatureException.class);
        }

        @Test
        @DisplayName("unsigned token is refused (the 'alg=none' attack)")
        void unsigned() {
            assertThatThrownBy(() -> parser.extractAllClaims(unsignedToken()))
                    .isInstanceOf(JwtException.class);
        }
    }

    // =========================================================
    // Subject and id
    // =========================================================

    @Nested
    @DisplayName("subject and id")
    class SubjectAndId {

        @Test
        @DisplayName("extractSubject / extractId return the raw strings")
        void rawValues() {
            assertThat(parser.extractSubject(validToken)).isEqualTo(SUBJECT.toString());
            assertThat(parser.extractId(validToken)).isEqualTo(TOKEN_ID.toString());
        }

        @Test
        @DisplayName("extractSubjectAsUuid parses a UUID subject")
        void subjectAsUuid() {
            assertThat(parser.extractSubjectAsUuid(validToken)).isEqualTo(SUBJECT);
        }

        @Test
        @DisplayName("extractSubjectAsUuid: a subject that is not a UUID gives JwtException with the cause")
        void subjectNotUuid() {
            assertThatThrownBy(() -> parser.extractSubjectAsUuid(tokenWithSubject("not-a-uuid")))
                    .isInstanceOf(JwtException.class)
                    .hasMessageContaining("Invalid UUID format")
                    .hasCauseInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("extractSubjectAsUuid: no subject gives JwtException")
        void subjectMissing() {
            assertThatThrownBy(() -> parser.extractSubjectAsUuid(tokenWithoutSubject()))
                    .isInstanceOf(JwtException.class)
                    .hasMessageContaining("valid subject");
        }

        @Test
        @DisplayName("extractIdAsUuid parses a UUID id")
        void idAsUuid() {
            assertThat(parser.extractIdAsUuid(validToken)).isEqualTo(TOKEN_ID);
        }

        @Test
        @DisplayName("extractIdAsUuid: an id that is not a UUID gives JwtException with the cause")
        void idNotUuid() {
            assertThatThrownBy(() -> parser.extractIdAsUuid(tokenWithId("not-a-uuid")))
                    .isInstanceOf(JwtException.class)
                    .hasMessageContaining("Invalid UUID format")
                    .hasCauseInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("extractIdAsUuid: no id gives JwtException")
        void idMissing() {
            String noId = JwtTokenBuilder.generateToken(SECRET, EXPIRATION, ISSUER, SUBJECT.toString());

            assertThatThrownBy(() -> parser.extractIdAsUuid(noId))
                    .isInstanceOf(JwtException.class)
                    .hasMessageContaining("ID claim");
        }

        @Test
        @DisplayName("an invalid token fails before any parsing of the subject")
        void invalidTokenFailsFirst() {
            assertThatThrownBy(() -> parser.extractSubjectAsUuid(forgedToken()))
                    .isInstanceOf(SignatureException.class);
        }
    }

    // =========================================================
    // Custom claims
    // =========================================================

    @Nested
    @DisplayName("extractClaim")
    class ExtractClaim {

        @Test
        @DisplayName("returns a custom claim, untyped and typed")
        void customClaim() {
            assertThat(parser.extractClaim(validToken, "role")).isEqualTo("ADMIN");
            assertThat(parser.extractClaim(validToken, "role", String.class)).isEqualTo("ADMIN");
            assertThat(parser.extractClaim(validToken, "tokenVersion", Integer.class)).isEqualTo(3);
        }

        @Test
        @DisplayName("an absent claim is null")
        void absentClaim() {
            assertThat(parser.extractClaim(validToken, "missing")).isNull();
            assertThat(parser.extractClaim(validToken, "missing", String.class)).isNull();
        }

        @Test
        @DisplayName("typed extraction with the wrong type fails with a JwtException")
        void wrongType() {
            assertThatThrownBy(() -> parser.extractClaim(validToken, "role", Integer.class))
                    .isInstanceOf(JwtException.class);
        }

        @ParameterizedTest(name = "claim name [{0}] is rejected")
        @NullAndEmptySource
        @DisplayName("a null or empty claim name is rejected")
        void invalidClaimName(String name) {
            assertThatThrownBy(() -> parser.extractClaim(validToken, name))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> parser.extractClaim(validToken, name, String.class))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("an invalid token fails the extraction")
        void invalidToken() {
            assertThatThrownBy(() -> parser.extractClaim(forgedToken(), "role"))
                    .isInstanceOf(SignatureException.class);
        }
    }

    // =========================================================
    // Dates
    // =========================================================

    @Nested
    @DisplayName("dates")
    class Dates {

        @Test
        @DisplayName("expiration is issuedAt + expirationMillis")
        void expirationAfterIssuedAt() {
            Date issuedAt = parser.extractIssuedAt(validToken);
            Date expiration = parser.extractExpiration(validToken);

            assertThat(expiration.getTime() - issuedAt.getTime()).isEqualTo(EXPIRATION);
        }

        @Test
        @DisplayName("isTokenExpired is false for a fresh token")
        void notExpired() {
            assertThat(parser.isTokenExpired(validToken)).isFalse();
        }

        /**
         * Pins today's behavior: the parser rejects expired tokens while verifying them,
         * so isTokenExpired can never return true for a real token: it throws.
         */
        @Test
        @DisplayName("isTokenExpired throws for an expired token instead of returning true")
        void expiredThrows() {
            assertThatThrownBy(() -> parser.isTokenExpired(expiredToken()))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }

    // =========================================================
    // Constructors / charset
    // =========================================================

    @Nested
    @DisplayName("constructors")
    class Constructors {

        @Test
        @DisplayName("an explicit ISO-8859-1 charset reads a plain ASCII secret the same way")
        void asciiSecretIsCharsetIndependent() {
            JwtTokenParser latin1 = new JwtTokenParser(
                    SECRET, StandardCharsets.ISO_8859_1, EXPIRATION, ISSUER);

            assertThat(latin1.validateToken(validToken)).isTrue();
        }

        @Test
        @DisplayName("a non-ASCII secret read with another charset gives another key: token rejected")
        void nonAsciiSecretNeedsSameCharset() {
            String secret = "ñ".repeat(20) + "abcdefghijklmnopqrst";
            String token = JwtTokenBuilder.generateToken(secret, EXPIRATION, ISSUER, SUBJECT.toString());

            assertThat(new JwtTokenParser(secret, EXPIRATION, ISSUER).validateToken(token)).isTrue();
            assertThat(new JwtTokenParser(secret, StandardCharsets.ISO_8859_1, EXPIRATION, ISSUER)
                    .validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("a secret shorter than 32 bytes is rejected")
        void shortSecret() {
            assertThatThrownBy(() -> new JwtTokenParser("too-short", EXPIRATION, ISSUER))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
