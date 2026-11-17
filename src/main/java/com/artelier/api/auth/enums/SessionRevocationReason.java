package com.artelier.api.auth.enums;

/**
 * Enumeration representing the various business rules and security reasons
 * why a user session has been terminated or revoked.
 */
public enum SessionRevocationReason {

    /**
     * The session was explicitly terminated due to a standard user logout action.
     */
    USER_LOGOUT,

    /**
     * The session was terminated as part of a global logout across all active devices.
     */
    LOGOUT_ALL,

    /**
     * The session was revoked because the user successfully updated their password.
     */
    PASSWORD_CHANGED,

    /**
     * The session was terminated because the underlying user account was suspended or deactivated.
     */
    ACCOUNT_SUSPENDED,

    /**
     * The session naturally expired after reaching its maximum allowed lifetime.
     */
    SESSION_EXPIRED,

    /**
     * The session was revoked due to a security trigger, such as a refresh token reuse attack or compromise detection.
     */
    SECURITY_ACTION,

    /**
     * The session was terminated manually by an administrator or system operator.
     */
    ADMIN_ACTION
}