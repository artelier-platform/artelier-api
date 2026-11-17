package com.artelier.api.integration.mail.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object (DTO) representing the payload required to send transactional emails via Brevo.
 * Maps to the {@code SendSmtpEmail} model from Brevo's official API specification.
 *
 * @param sender          Information about the sender (optional).
 * @param to              List of recipient email addresses and names. Mandatory if messageVersions are not passed (optional).
 * @param bccList         List of Bcc recipient email addresses and names (optional).
 * @param ccList          List of Cc recipient email addresses and names (optional).
 * @param htmlContent     HTML body of the message. Mandatory if 'templateId' is not passed (optional).
 * @param textContent     Plain text body of the message (optional).
 * @param subject         Subject of the message. Mandatory if 'templateId' is not passed (optional).
 * @param replyTo         Reply-to configuration (optional).
 * @param attachmentList  List of attachments with absolute URLs or base64 contents (optional).
 * @param headers         Custom headers to be sent along with the email (optional).
 * @param templateId      ID of the email template in Brevo (optional).
 * @param params          Key-value attributes to customize templates using New Template Language (optional).
 * @param messageVersions List of custom individual versions for batch sending up to 2000 total recipients (optional).
 * @param tags            Tags to categorize and filter emails (optional).
 * @param scheduledAt     UTC date-time on which the email is scheduled to be sent (optional).
 * @param batchId         Valid UUIDv4 batch ID for scheduled emails (optional).
 * @param preheader       Short summary preview text that appears next to the subject line (optional).
 */
@Builder
public record SendSmtpEmail(

        @Valid
        SendSmtpEmailSender sender,

        @Valid
        List<SendSmtpEmailTo> to,

        @Valid
        List<SendSmtpEmailBcc> bccList,

        @Valid
        List<SendSmtpEmailCc> ccList,

        String htmlContent,

        String textContent,

        String subject,

        @Valid
        SendSmtpEmailReplyTo replyTo,

        @Valid
        List<SendSmtpEmailAttachment> attachmentList,

        Map<String, Object> headers,

        Long templateId,

        Map<String, Object> params,

        @Valid
        List<SendSmtpEmailMessageVersions> messageVersions,

        List<String> tags,

        OffsetDateTime scheduledAt,

        @Size(min = 36, max = 36, message = "Batch ID must be a valid UUIDv4 string")
        String batchId,

        String preheader

) {}