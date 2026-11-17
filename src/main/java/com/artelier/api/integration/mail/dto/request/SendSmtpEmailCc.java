package com.artelier.api.integration.mail.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Data Transfer Object (DTO) representing a Cc recipient for Brevo SMTP emails.
 * Maps to the {@code SendSmtpEmailCc} model from Brevo's official API specification.
 *
 * @param email                      Email address of the recipient in cc (required).
 * @param name                       Name of the recipient in cc. Maximum allowed characters are 70 (optional).
 * @param contactPixelTrackingConsent Consent of the recipient in cc for open and click tracking (optional).
 */
@Builder
public record SendSmtpEmailCc(

        @NotBlank(message = "Cc recipient email cannot be blank")
        @Email(message = "Cc recipient email format is invalid")
        String email,

        @Size(max = 70, message = "Cc recipient name cannot exceed 70 characters")
        String name,

        Boolean contactPixelTrackingConsent

) {}