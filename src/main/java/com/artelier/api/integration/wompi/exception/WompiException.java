package com.artelier.api.integration.wompi.exception;

import com.artelier.api.exception.ArtelierException;
import com.artelier.api.integration.wompi.enums.WompiErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class WompiException extends ArtelierException {

    private final String wompiCode;

    public WompiException(String wompiCode) {
        super(
                WompiErrorCode.fromCode(wompiCode).getDefaultMessage(),
                HttpStatus.BAD_REQUEST
        );
        this.wompiCode = wompiCode;
    }

    public WompiException(String wompiCode, String customMessage) {
        super(customMessage, HttpStatus.BAD_REQUEST);
        this.wompiCode = wompiCode;
    }

    public WompiException(String wompiCode, String customMessage, HttpStatus status) {
        super(customMessage, status);
        this.wompiCode = wompiCode;
    }
}