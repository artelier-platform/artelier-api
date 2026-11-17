package com.artelier.api.integration.mail.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object (DTO) representing a specific message version for batch sending emails via Brevo.
 * Maps to the {@code SendSmtpEmailMessageVersions} model from Brevo's official API specification.
 *
 * @param to          List of recipient email addresses and names (required).
 * @param params      Attributes to customize the template for this version (optional).
 * @param bccList     List of Bcc recipients for this version (optional).
 * @param ccList      List of Cc recipients for this version (optional).
 * @param replyTo     Reply-to configuration for this version (optional).
 * @param subject     Custom subject specific to this message version (optional).
 * @param htmlContent HTML body of the message for this version (optional).
 * @param textContent Plain text body of the message for this version (optional).
 * @param headers     Custom headers for this message version (optional).
 */
@Builder
public record SendSmtpEmailMessageVersions(

        @NotEmpty(message = "Recipient list in message version cannot be empty")
        @Valid
        List<SendSmtpEmailTo> to,

        Map<String, Object> params,

        @Valid
        List<SendSmtpEmailBcc> bccList,

        @Valid
        List<SendSmtpEmailCc> ccList,

        @Valid
        SendSmtpEmailReplyTo replyTo,

        String subject,

        String htmlContent,

        String textContent,

        Map<String, Object> headers

) {}