package com.artelier.api.integration.mail.dto.request;

import lombok.Builder;
import org.hibernate.validator.constraints.URL;

/**
 * Data Transfer Object (DTO) representing an attachment for Brevo SMTP emails.
 * Maps to the {@code SendSmtpEmailAttachment} model from Brevo's official API specification.
 *
 * @param url     Absolute URL of the attachment (no local file) (optional).
 * @param content Base64 encoded chunk data of the attachment generated on the fly (optional).
 * @param name    Name of the attachment. Required if {@code content} is passed (optional).
 */
@Builder
public record SendSmtpEmailAttachment(

        @URL(message = "Attachment URL must be a valid absolute URL")
        String url,

        byte[] content,

        String name

) {}