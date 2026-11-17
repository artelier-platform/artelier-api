package com.artelier.api.shared.util;

import com.artelier.api.auth.exception.HashingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Hasher")
class HasherTest {

    @ParameterizedTest(name = "{0}(''{1}'') = {2}")
    @CsvSource({
            "SHA-256, abc, ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            "SHA-256, '', e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            "SHA-1,   abc, a9993e364706816aba3e25717850c26c9cd0d89d",
            "MD5,     abc, 900150983cd24fb0d6963f7d28e17f72",
            "SHA-512, abc, ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f"
    })
    @DisplayName("matches the standard test vectors")
    void knownVectors(String algorithm, String data, String expected) {
        assertThat(Hasher.hash(algorithm, data, StandardCharsets.UTF_8)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} -> {1} hex chars")
    @CsvSource({"SHA-256, 64", "SHA-512, 128", "MD5, 32"})
    @DisplayName("output is lowercase hex with the digest's full length")
    void lowercaseHexAndLength(String algorithm, int length) {
        String hash = Hasher.hash(algorithm, "any data", StandardCharsets.UTF_8);

        assertThat(hash).hasSize(length).matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("is deterministic")
    void deterministic() {
        assertThat(Hasher.hash("SHA-256", "same", StandardCharsets.UTF_8))
                .isEqualTo(Hasher.hash("SHA-256", "same", StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("the charset matters for non-ASCII text")
    void charsetMatters() {
        String text = "ñandú";

        assertThat(Hasher.hash("SHA-256", text, StandardCharsets.UTF_8))
                .isNotEqualTo(Hasher.hash("SHA-256", text, StandardCharsets.ISO_8859_1));
    }

    @Test
    @DisplayName("the charset does not matter for plain ASCII")
    void charsetIrrelevantForAscii() {
        assertThat(Hasher.hash("SHA-256", "abc", StandardCharsets.UTF_8))
                .isEqualTo(Hasher.hash("SHA-256", "abc", StandardCharsets.ISO_8859_1));
    }

    @Test
    @DisplayName("unknown algorithm: HashingException naming it, with the original cause")
    void unknownAlgorithm() {
        assertThatThrownBy(() -> Hasher.hash("SHA-999", "abc", StandardCharsets.UTF_8))
                .isInstanceOf(HashingException.class)
                .hasMessageContaining("SHA-999")
                .hasCauseInstanceOf(NoSuchAlgorithmException.class);
    }

    @Test
    @DisplayName("null data is rejected with NullPointerException")
    void nullData() {
        assertThatThrownBy(() -> Hasher.hash("SHA-256", null, StandardCharsets.UTF_8))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("utility class: the constructor is private")
    void privateConstructor() throws Exception {
        Constructor<Hasher> constructor = Hasher.class.getDeclaredConstructor();

        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();

        constructor.setAccessible(true);
        assertThat(constructor.newInstance()).isNotNull();
    }
}
