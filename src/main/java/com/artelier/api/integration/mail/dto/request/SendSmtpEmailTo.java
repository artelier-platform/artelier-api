package com.artelier.api.integration.mail.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Data Transfer Object (DTO) representing a recipient for Brevo SMTP emails.
 * Maps to the {@code SendSmtpEmailTo} model from Brevo's official API specification.
 *
 * @param email                      Email address of the recipient (required).
 * @param name                       Name of the recipient. Maximum allowed characters are 70 (optional).
 * @param contactPixelTrackingConsent Consent of the recipient for open and click tracking (optional).
 */
@Builder
public record SendSmtpEmailTo(

        @NotBlank(message = "Recipient email cannot be blank")
        @Email(message = "Recipient email format is invalid")
        String email,

        @Size(max = 70, message = "Recipient name cannot exceed 70 characters")
        String name,

        Boolean contactPixelTrackingConsent

) {}