package com.artelier.api.security;

import com.artelier.api.util.Hasher;
import java.nio.charset.StandardCharsets;

/**
 * Handles the hashing of refresh tokens.
 * Uses SHA-256 with UTF-8 encoding to generate deterministic hashes
 * that can be safely stored instead of raw refresh tokens.
 */
public class RefreshTokenHasher {

    /**
     * Generates a SHA-256 hash from a raw refresh token.
     *
     * @param rawToken the original refresh token
     * @return the hexadecimal representation of the token's SHA-256 hash
     */
    public String hash(String rawToken) {
        return Hasher.hash(
                "SHA-256",
                rawToken,
                StandardCharsets.UTF_8
        );
    }
}