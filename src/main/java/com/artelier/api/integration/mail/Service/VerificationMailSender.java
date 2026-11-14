package com.artelier.api.integration.mail.Service;

import com.artelier.api.integration.mail.dto.response.CreateSmtpEmail;

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
     * @return a {@link CreateSmtpEmail} response object containing details of the sent transaction
     */
    CreateSmtpEmail sendEmailVerification(String to, String token);
}