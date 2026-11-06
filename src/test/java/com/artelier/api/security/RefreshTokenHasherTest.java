package com.artelier.api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}