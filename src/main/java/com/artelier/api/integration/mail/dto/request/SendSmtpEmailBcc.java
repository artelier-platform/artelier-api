package com.artelier.api.integration.mail.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Data Transfer Object (DTO) representing a Bcc recipient for Brevo SMTP emails.
 * Maps to the {@code SendSmtpEmailBcc} model from Brevo's official API specification.
 *
 * @param email                      Email address of the recipient in bcc (required).
 * @param name                       Name of the recipient in bcc. Maximum allowed characters are 70 (optional).
 * @param contactPixelTrackingConsent Consent of the recipient for open and click tracking (optional).
 */
@Builder
public record SendSmtpEmailBcc(

        @NotBlank(message = "Bcc recipient email cannot be blank")
        @Email(message = "Bcc recipient email format is invalid")
        String email,

        @Size(max = 70, message = "Bcc recipient name cannot exceed 70 characters")
        String name,

        Boolean contactPixelTrackingConsent

) {}