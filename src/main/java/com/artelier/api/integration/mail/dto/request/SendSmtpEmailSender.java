package com.artelier.api.integration.mail.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Data Transfer Object (DTO) representing the sender of a Brevo SMTP email.
 * Maps to the {@code SendSmtpEmailSender} model from Brevo's official API specification.
 *
 * @param name  Name of the sender from which the emails will be sent. Maximum allowed characters are 70 (optional).
 * @param email Email of the sender from which the emails will be sent. Mandatory if sender ID is not passed (optional).
 * @param id    ID of the sender from which the emails will be sent. Used for dedicated IP pools. Mandatory if email is not passed (optional).
 */
@Builder
public record SendSmtpEmailSender(

        @Size(max = 70, message = "Sender name cannot exceed 70 characters")
        String name,

        @Email(message = "Sender email format is invalid")
        String email,

        Long id

) {}