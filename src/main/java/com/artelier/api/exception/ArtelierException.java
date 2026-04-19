package com.artelier.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ArtelierException extends RuntimeException {

    private final HttpStatus status;

    public ArtelierException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }


    public static ArtelierException notFound(String message) {
        return new ArtelierException(message, HttpStatus.NOT_FOUND);
    }

    public static ArtelierException unauthorized(String message) {
        return new ArtelierException(message, HttpStatus.UNAUTHORIZED);
    }

    public static ArtelierException conflict(String message) {
        return new ArtelierException(message, HttpStatus.CONFLICT);
    }

    public static ArtelierException badRequest(String message) {
        return new ArtelierException(message, HttpStatus.BAD_REQUEST);
    }

    public static ArtelierException forbidden(String message) {
        return new ArtelierException(message, HttpStatus.FORBIDDEN);
    }
}
