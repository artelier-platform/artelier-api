package com.artelier.api.shared.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility generator class responsible for producing cryptographically secure,
 * URL-safe opaque refresh tokens.
 *
 * <p>Utilizes a cryptographically strong pseudo-random number generator ({@link SecureRandom})
 * and URL-safe Base64 encoding without padding.</p>
 */
public final class OpaqueRefreshTokenGenerator {

    /**
     * Cryptographically secure pseudo-random number generator instance for token generation.
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private OpaqueRefreshTokenGenerator() {
    }

    /**
     * Generates a new cryptographically secure, URL-safe opaque refresh token string.
     *
     * @return a random 256-bit (32 bytes) encoded string suitable for use as a refresh token
     */
    public static String generate() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}