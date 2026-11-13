package com.artelier.api.service;

import com.artelier.api.dto.response.UserEmailResponse;
import com.artelier.api.entity.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing user email addresses, handling secondary email additions,
 * verification tokens, setting primary contact channels, and removing associated emails.
 */
public interface UserEmailService {

    /**
     * Adds a new email address to the specified user account.
     *
     * @param userId the unique identifier of the user
     * @param email  the new email address to add
     * @return a {@link UserEmailResponse} containing the details of the added email
     */
    UserEmailResponse addEmail(UUID userId, String email);

    /**
     * Finds and returns the verified owner associated with a given email address.
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the verified {@link User} owner if found
     */
    Optional<User> findVerifiedOwner(String email);

    /**
     * Verifies an email address using the provided verification token.
     *
     * @param token the cryptographic or time-sensitive verification token sent to the email
     * @return a {@link UserEmailResponse} containing the updated status of the verified email
     */
    UserEmailResponse verifyEmail(String token);

    /**
     * Sets a specific email address as the primary contact channel for the user.
     *
     * @param userId  the unique identifier of the user
     * @param emailId the unique identifier of the email address to designate as primary
     * @return a {@link UserEmailResponse} containing the updated email association details
     */
    UserEmailResponse setPrimary(UUID userId, UUID emailId);

    /**
     * Removes an associated email address from the user account.
     *
     * @param userId  the unique identifier of the user
     * @param emailId the unique identifier of the email address to remove
     */
    void removeEmail(UUID userId, UUID emailId);
}