package com.artelier.api.integration.mail.Service.impl;

import com.artelier.api.integration.mail.Service.BrevoService;
import com.artelier.api.integration.mail.Service.VerificationMailSender;
import com.artelier.api.integration.mail.dto.request.SendSmtpEmail;
import com.artelier.api.integration.mail.dto.request.SendSmtpEmailSender;
import com.artelier.api.integration.mail.dto.request.SendSmtpEmailTo;
import com.artelier.api.integration.mail.dto.response.CreateSmtpEmail;
import com.artelier.api.user.repository.UserEmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link VerificationMailSender} responsible for constructing
 * and dispatching transactional email verification messages via the Brevo API.
 *
 * <p>Retrieves user details by email, formats verification links using application configuration,
 * and populates Brevo transactional templates with dynamic parameters.</p>
 */
@Service
@RequiredArgsConstructor
public class VerificationMailSenderImpl implements VerificationMailSender {

    /**
     * The base URL used for constructing verification links.
     */
    @Value("${app.jwt.issuer}")
    private String baseUrl;

    /**
     * The Brevo template ID used for authentication and account verification emails.
     */
    @Value("${brevo.templates.auth-verification}")
    private Long authVerificationTemplateId;

    /**
     * The display name of the email sender.
     */
    @Value("${brevo.sender.name}")
    private String senderName;

    /**
     * The email address of the sender.
     */
    @Value("${brevo.sender.email}")
    private String senderEmail;

    /**
     * The Brevo transactional mail service client.
     */
    private final BrevoService brevoService;

    /**
     * Repository for accessing user email records and associated account data.
     */
    private final UserEmailRepository userEmailRepository;

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if no user email record is found for the specified recipient address
     */
    @Override
    public CreateSmtpEmail sendEmailVerification(String to, String token) {
        String verificationLink = baseUrl + "/user/emails/verify?token=" + token;

        SendSmtpEmailSender sender = SendSmtpEmailSender.builder()
                .name(senderName)
                .email(senderEmail)
                .build();

        String username = userEmailRepository.findByEmail(to)
                .map(userEmail -> userEmail.getUser().getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Email no encontrado: " + to));

        List<SendSmtpEmailTo> recipients = List.of(
                SendSmtpEmailTo.builder()
                        .email(to)
                        .name(username)
                        .build()
        );

        Map<String, Object> templateParams = Map.of(
                "USER_NAME", username,
                "VERIFICATION_LINK", verificationLink
        );

        SendSmtpEmail sendSmtpEmail = SendSmtpEmail.builder()
                .sender(sender)
                .to(recipients)
                .templateId(authVerificationTemplateId)
                .params(templateParams)
                .build();

        return brevoService.sendTransacEmail(sendSmtpEmail);
    }
}