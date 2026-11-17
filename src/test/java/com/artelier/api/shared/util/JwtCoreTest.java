package com.artelier.api.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtCore is abstract: an empty anonymous subclass is enough to exercise it.
 * Same package as JwtCore on purpose, so the protected fields are readable.
 */
@DisplayName("JwtCore")
class JwtCoreTest {

    private static final String ISSUER = "https://api.artelier.test";
    private static final long EXPIRATION = 900_000L;

    @Test
    @DisplayName("accepts a secret of exactly 32 bytes")
    void exactly32Bytes() {
        JwtCore core = new JwtCore("a".repeat(32), EXPIRATION, ISSUER) {};

        assertThat(core.secretKey).isNotNull();
    }

    @Test
    @DisplayName("rejects a secret of 31 bytes")
    void tooShort() {
        assertThatThrownBy(() -> new JwtCore("a".repeat(31), EXPIRATION, ISSUER) {})
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("256 bits");
    }

    @Test
    @DisplayName("the 32-byte minimum counts bytes, not characters")
    void countsBytesNotChars() {
        String sixteenChars = "á".repeat(16);

        // 16 chars = 32 bytes in UTF-8, but only 16 bytes in ISO-8859-1.
        assertThat(new JwtCore(sixteenChars, StandardCharsets.UTF_8, EXPIRATION, ISSUER) {}.secretKey)
                .isNotNull();
        assertThatThrownBy(() ->
                new JwtCore(sixteenChars, StandardCharsets.ISO_8859_1, EXPIRATION, ISSUER) {})
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a null charset falls back to UTF-8")
    void nullCharsetDefaultsToUtf8() {
        String secret = "ñ".repeat(20) + "abcdefghijklmnopqrst";

        JwtCore core = new JwtCore(secret, null, EXPIRATION, ISSUER) {};

        assertThat(core.secretKey.getEncoded()).isEqualTo(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("the short constructor uses UTF-8")
    void shortConstructorUsesUtf8() {
        String secret = "ñ".repeat(20) + "abcdefghijklmnopqrst";

        JwtCore core = new JwtCore(secret, EXPIRATION, ISSUER) {};

        assertThat(core.secretKey.getEncoded()).isEqualTo(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("the HMAC algorithm follows the key size")
    void algorithmFollowsKeySize() {
        assertThat(new JwtCore("a".repeat(32), EXPIRATION, ISSUER) {}.secretKey.getAlgorithm())
                .isEqualTo("HmacSHA256");
        assertThat(new JwtCore("a".repeat(64), EXPIRATION, ISSUER) {}.secretKey.getAlgorithm())
                .isEqualTo("HmacSHA512");
    }

    @Test
    @DisplayName("keeps the configured expiration, issuer and secret")
    void keepsConfiguration() {
        JwtCore core = new JwtCore("a".repeat(32), EXPIRATION, ISSUER) {};

        assertThat(core.getExpirationMillis()).isEqualTo(EXPIRATION);
        assertThat(core.issuer).isEqualTo(ISSUER);
        assertThat(core.secret).isEqualTo("a".repeat(32));
    }

    @Test
    @DisplayName("null secret is rejected with NullPointerException")
    void nullSecret() {
        assertThatThrownBy(() -> new JwtCore(null, EXPIRATION, ISSUER) {})
                .isInstanceOf(NullPointerException.class);
    }
}
