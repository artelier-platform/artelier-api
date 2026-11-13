package com.artelier.api.service;

import com.artelier.api.enums.UserStatus;

import java.util.UUID;

/**
 * Service contract for managing user account verification and eligibility-based status transitions.
 */
public interface AccountVerificationService {

    /**
     * Evaluates whether a user account meets the criteria for activation (e.g., verified email)
     * and transitions its status if eligible.
     *
     * @param userId the unique identifier of the user account to evaluate
     * @return the updated {@link UserStatus} of the account after processing
     */
    UserStatus activateIfEligible(UUID userId);
}