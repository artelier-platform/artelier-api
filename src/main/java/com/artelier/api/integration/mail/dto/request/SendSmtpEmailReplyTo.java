package com.artelier.api.integration.mail.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Data Transfer Object (DTO) representing the reply-to configuration for Brevo SMTP emails.
 * Maps to the {@code SendSmtpEmailReplyTo} model from Brevo's official API specification.
 *
 * @param email Email address to reply to (required).
 * @param name  Name in reply to. Maximum allowed characters are 70 (optional).
 */
@Builder
public record SendSmtpEmailReplyTo(

        @NotBlank(message = "Reply-to email cannot be blank")
        @Email(message = "Reply-to email format is invalid")
        String email,

        @Size(max = 70, message = "Reply-to name cannot exceed 70 characters")
        String name

) {}