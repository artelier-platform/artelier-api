package com.artelier.api.integration.wompi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WompiErrorCode {

    D02("The selected bank does not exist."),
    D07("The destination account does not exist."),
    D19("Insufficient funds."),
    D39("Invalid identification document type."),
    D49("The selected bank is not allowed."),
    UNKNOWN("Payment could not be processed.");

    private final String defaultMessage;

    public static WompiErrorCode fromCode(String code) {
        for (WompiErrorCode value : values()) {
            if (value.name().equalsIgnoreCase(code)) {
                return value;
            }
        }
        return UNKNOWN;
    }
}