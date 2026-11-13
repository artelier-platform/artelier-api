package com.artelier.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Custom runtime exception class for handling business logic and API error scenarios
 * within the Artelier application.
 *
 * <p>Encapsulates an associated HTTP status code alongside the error message
 * to facilitate centralized global exception handling and standardized error responses,
 * with built-in support for string-formatted message generation.</p>
 */
@Getter
public class ArtelierException extends RuntimeException {

    /**
     * The HTTP status code associated with this exception.
     */
    private final HttpStatus status;

    /**
     * Constructs a new {@code ArtelierException} with the specified detail message and HTTP status.
     *
     * @param message the detail message explaining the reason for the exception
     * @param status  the HTTP status code to be returned in the response
     */
    public ArtelierException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    // --- NOT FOUND (404) ---

    /**
     * Creates an {@code ArtelierException} representing a resource not found error (HTTP 404).
     *
     * @param message the detail message
     * @return a new {@code ArtelierException} with status {@link HttpStatus#NOT_FOUND}
     */
    public static ArtelierException notFound(String message) {
        return new ArtelierException(message, HttpStatus.NOT_FOUND);
    }

    /**
     * Creates a formatted {@code ArtelierException} representing a resource not found error (HTTP 404).
     *
     * @param message the format string message
     * @param args    arguments referenced by the format specifiers in the message string
     * @return a new formatted {@code ArtelierException} with status {@link HttpStatus#NOT_FOUND}
     */
    public static ArtelierException notFound(String message, Object... args) {
        return new ArtelierException(String.format(message, args), HttpStatus.NOT_FOUND);
    }

    // --- UNAUTHORIZED (401) ---

    /**
     * Creates an {@code ArtelierException} representing an unauthorized access error (HTTP 401).
     *
     * @param message the detail message
     * @return a new {@code ArtelierException} with status {@link HttpStatus#UNAUTHORIZED}
     */
    public static ArtelierException unauthorized(String message) {
        return new ArtelierException(message, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Creates a formatted {@code ArtelierException} representing an unauthorized access error (HTTP 401).
     *
     * @param message the format string message
     * @param args    arguments referenced by the format specifiers in the message string
     * @return a new formatted {@code ArtelierException} with status {@link HttpStatus#UNAUTHORIZED}
     */
    public static ArtelierException unauthorized(String message, Object... args) {
        return new ArtelierException(String.format(message, args), HttpStatus.UNAUTHORIZED);
    }

    // --- CONFLICT (409) ---

    /**
     * Creates an {@code ArtelierException} representing a data or state conflict (HTTP 409).
     *
     * @param message the detail message
     * @return a new {@code ArtelierException} with status {@link HttpStatus#CONFLICT}
     */
    public static ArtelierException conflict(String message) {
        return new ArtelierException(message, HttpStatus.CONFLICT);
    }

    /**
     * Creates a formatted {@code ArtelierException} representing a data or state conflict (HTTP 409).
     *
     * @param message the format string message
     * @param args    arguments referenced by the format specifiers in the message string
     * @return a new formatted {@code ArtelierException} with status {@link HttpStatus#CONFLICT}
     */
    public static ArtelierException conflict(String message, Object... args) {
        return new ArtelierException(String.format(message, args), HttpStatus.CONFLICT);
    }

    // --- BAD REQUEST (400) ---

    /**
     * Creates an {@code ArtelierException} representing a malformed or invalid client request (HTTP 400).
     *
     * @param message the detail message
     * @return a new {@code ArtelierException} with status {@link HttpStatus#BAD_REQUEST}
     */
    public static ArtelierException badRequest(String message) {
        return new ArtelierException(message, HttpStatus.BAD_REQUEST);
    }

    /**
     * Creates a formatted {@code ArtelierException} representing a malformed or invalid client request (HTTP 400).
     *
     * @param message the format string message
     * @param args    arguments referenced by the format specifiers in the message string
     * @return a new formatted {@code ArtelierException} with status {@link HttpStatus#BAD_REQUEST}
     */
    public static ArtelierException badRequest(String message, Object... args) {
        return new ArtelierException(String.format(message, args), HttpStatus.BAD_REQUEST);
    }

    // --- FORBIDDEN (403) ---

    /**
     * Creates an {@code ArtelierException} representing a forbidden action due to insufficient permissions (HTTP 403).
     *
     * @param message the detail message
     * @return a new {@code ArtelierException} with status {@link HttpStatus#FORBIDDEN}
     */
    public static ArtelierException forbidden(String message) {
        return new ArtelierException(message, HttpStatus.FORBIDDEN);
    }

    /**
     * Creates a formatted {@code ArtelierException} representing a forbidden action due to insufficient permissions (HTTP 403).
     *
     * @param message the format string message
     * @param args    arguments referenced by the format specifiers in the message string
     * @return a new formatted {@code ArtelierException} with status {@link HttpStatus#FORBIDDEN}
     */
    public static ArtelierException forbidden(String message, Object... args) {
        return new ArtelierException(String.format(message, args), HttpStatus.FORBIDDEN);
    }

    // --- INTERNAL SERVER ERROR (500) ---

    /**
     * Creates an {@code ArtelierException} representing an unexpected internal server error (HTTP 500).
     *
     * @param message the detail message
     * @return a new {@code ArtelierException} with status {@link HttpStatus#INTERNAL_SERVER_ERROR}
     */
    public static ArtelierException internalServerError(String message) {
        return new ArtelierException(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Creates a formatted {@code ArtelierException} representing an unexpected internal server error (HTTP 500).
     *
     * @param message the format string message
     * @param args    arguments referenced by the format specifiers in the message string
     * @return a new formatted {@code ArtelierException} with status {@link HttpStatus#INTERNAL_SERVER_ERROR}
     */
    public static ArtelierException internalServerError(String message, Object... args) {
        return new ArtelierException(String.format(message, args), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}