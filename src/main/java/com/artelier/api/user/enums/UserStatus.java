package com.artelier.api.user.enums;

/**
 * Enumeration representing the lifecycle status of a user account within the Artelier application.
 */
public enum UserStatus {

    /**
     * Account has been created but is pending email address verification.
     */
    PENDING_VERIFICATION,

    /**
     * Account is fully verified, active, and authorized to access system features.
     */
    ACTIVE,

    /**
     * Account has been suspended due to policy violations, administrative action, or security concerns.
     */
    SUSPENDED
}