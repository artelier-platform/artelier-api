package com.artelier.api.auth.security;

import com.artelier.api.auth.util.RefreshTokenHasher;
import com.artelier.api.shared.util.Hasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RefreshTokenHasher")
class RefreshTokenHasherTest {

    private final RefreshTokenHasher hasher = new RefreshTokenHasher();

    @Test
    @DisplayName("is SHA-256 in lowercase hex (standard test vector)")
    void knownVector() {
        assertThat(hasher.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    @DisplayName("is deterministic: the same token always gives the same hash")
    void deterministic() {
        assertThat(hasher.hash("some-token")).isEqualTo(hasher.hash("some-token"));
    }

    @Test
    @DisplayName("different tokens give different hashes")
    void differentInputs() {
        assertThat(hasher.hash("token-a")).isNotEqualTo(hasher.hash("token-b"));
    }

    @Test
    @DisplayName("fits the refresh_token_hash column (255) and never contains the raw token")
    void fitsColumnAndHidesToken() {
        String raw = "a".repeat(100);

        String hash = hasher.hash(raw);

        assertThat(hash).hasSize(64).doesNotContain(raw);
    }

    // ---- added ----

    @Test
    @DisplayName("an empty token still hashes (standard SHA-256 of the empty string)")
    void emptyToken() {
        assertThat(hasher.hash(""))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    @DisplayName("delegates to Hasher with SHA-256 and UTF-8, not another charset")
    void usesSha256AndUtf8() {
        String token = "ñandú-€-token";

        assertThat(hasher.hash(token))
                .isEqualTo(Hasher.hash("SHA-256", token, StandardCharsets.UTF_8))
                .isNotEqualTo(Hasher.hash("SHA-256", token, StandardCharsets.ISO_8859_1));
    }

    @Test
    @DisplayName("a very long token still gives a 64-char hash")
    void longToken() {
        assertThat(hasher.hash("a".repeat(2048))).hasSize(64);
    }

    @Test
    @DisplayName("a null token is rejected with NullPointerException")
    void nullToken() {
        assertThatThrownBy(() -> hasher.hash(null)).isInstanceOf(NullPointerException.class);
    }
}
