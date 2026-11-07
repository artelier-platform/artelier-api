package com.artelier.api.util;

import com.artelier.api.exception.HashingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class that provides generic hashing functionality.
 * Uses MessageDigest to generate cryptographic hashes and HexFormat
 * to represent the resulting bytes as hexadecimal strings.
 */
public class Hasher {

    private Hasher() {
        /* This utility class should not be instantiated */
    }

    /**
     * Generates a hash from the provided data using the specified algorithm
     * and character encoding.
     *
     * @param algorithm the hashing algorithm to use (e.g. SHA-256)
     * @param data the input data to be hashed
     * @param charset the character encoding used to convert the input data into bytes
     * @return the hexadecimal representation of the generated hash
     * @throws HashingException if the specified hashing algorithm is unavailable
     */
    public static String hash(
            String algorithm,
            String data,
            Charset charset
    ) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);

            byte[] hashedBytes = md.digest(
                    data.getBytes(charset)
            );

            HexFormat hex = HexFormat.of();

            return hex.formatHex(hashedBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new HashingException(
                    "Hashing algorithm not available: " + algorithm,
                    e
            );
        }
    }
}