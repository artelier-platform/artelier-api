package com.artelier.api.integration.mail.dto.response;

import java.util.List;

/**
 * Data Transfer Object (DTO) representing the response returned after successfully sending a transactional email via Brevo.
 * Maps to the {@code CreateSmtpEmail} model from Brevo's official API specification.
 *
 * @param messageId  Message ID of the transactional email sent (optional).
 * @param messageIds List of message IDs returned for batch or multi-recipient emails (optional).
 */
public record CreateSmtpEmail(

        String messageId,

        List<String> messageIds

) {}