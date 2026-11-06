package com.artelier.api.enums;

public enum SessionRevocationReason {

    USER_LOGOUT,
    LOGOUT_ALL,
    PASSWORD_CHANGED,
    ACCOUNT_SUSPENDED,
    SESSION_EXPIRED,
    SECURITY_ACTION,
    ADMIN_ACTION
}