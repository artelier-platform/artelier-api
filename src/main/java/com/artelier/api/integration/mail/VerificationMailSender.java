package com.artelier.api.integration.mail;

/**
 * Service contract for sending email verification notifications and tokens
 * to users during registration or email update workflows.
 */
public interface VerificationMailSender {

    /**
     * Sends an email verification message containing the specified verification token
     * to the recipient's email address.
     *
     * @param to    the recipient's email address
     * @param token the secure verification token to be included in the email
     */
    void sendEmailVerification(String to, String token);
}