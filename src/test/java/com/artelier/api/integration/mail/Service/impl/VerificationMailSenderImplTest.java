package com.artelier.api.integration.mail.Service.impl;

import com.artelier.api.integration.mail.Service.BrevoService;
import com.artelier.api.integration.mail.dto.request.SendSmtpEmail;
import com.artelier.api.integration.mail.dto.response.CreateSmtpEmail;
import com.artelier.api.user.entity.User;
import com.artelier.api.user.entity.UserEmail;
import com.artelier.api.user.repository.UserEmailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests: Brevo and the repository are mocked, nothing leaves the JVM.
 * The wire format (JSON, headers, HTTP errors) is covered by the integration tests.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationMailSender")
class VerificationMailSenderImplTest {

    private static final String BASE_URL = "https://api.artelier.test";
    private static final Long TEMPLATE_ID = 7L;
    private static final String SENDER_NAME = "Artelier Cajica";
    private static final String SENDER_EMAIL = "no-reply@artelier.test";

    private static final String TO = "buyer@example.com";
    private static final String USERNAME = "Geronimo01";
    private static final String TOKEN = "aaa.bbb.ccc";

    @Mock private BrevoService brevoService;
    @Mock private UserEmailRepository userEmailRepository;

    @InjectMocks private VerificationMailSenderImpl sender;

    @BeforeEach
    void setUp() {
        // @Value fields are not populated outside a Spring context.
        ReflectionTestUtils.setField(sender, "baseUrl", BASE_URL);
        ReflectionTestUtils.setField(sender, "authVerificationTemplateId", TEMPLATE_ID);
        ReflectionTestUtils.setField(sender, "senderName", SENDER_NAME);
        ReflectionTestUtils.setField(sender, "senderEmail", SENDER_EMAIL);
    }

    // =========================================================
    // Fixtures
    // =========================================================

    private void givenOwner(String address, String username) {
        User user = User.builder().username(username).build();
        UserEmail userEmail = UserEmail.builder().email(address).user(user).build();
        when(userEmailRepository.findByEmail(address)).thenReturn(Optional.of(userEmail));
    }

    private SendSmtpEmail sentPayload() {
        ArgumentCaptor<SendSmtpEmail> captor = ArgumentCaptor.forClass(SendSmtpEmail.class);
        verify(brevoService).sendTransacEmail(captor.capture());
        return captor.getValue();
    }

    // =========================================================
    // sendEmailVerification
    // =========================================================

    @Nested
    @DisplayName("sendEmailVerification")
    class SendEmailVerification {

        @Test
        @DisplayName("sends exactly one recipient: the address, named after the owner's username")
        void recipient() {
            givenOwner(TO, USERNAME);

            sender.sendEmailVerification(TO, TOKEN);

            SendSmtpEmail payload = sentPayload();
            assertThat(payload.to()).hasSize(1);
            assertThat(payload.to().getFirst().email()).isEqualTo(TO);
            assertThat(payload.to().getFirst().name()).isEqualTo(USERNAME);
        }

        @Test
        @DisplayName("uses the configured sender (name and email)")
        void configuredSender() {
            givenOwner(TO, USERNAME);

            sender.sendEmailVerification(TO, TOKEN);

            SendSmtpEmail payload = sentPayload();
            assertThat(payload.sender().name()).isEqualTo(SENDER_NAME);
            assertThat(payload.sender().email()).isEqualTo(SENDER_EMAIL);
        }

        @Test
        @DisplayName("uses the Brevo template: no inline subject or body")
        void usesTemplate() {
            givenOwner(TO, USERNAME);

            sender.sendEmailVerification(TO, TOKEN);

            SendSmtpEmail payload = sentPayload();
            assertThat(payload.templateId()).isEqualTo(TEMPLATE_ID);
            assertThat(payload.subject()).isNull();
            assertThat(payload.htmlContent()).isNull();
            assertThat(payload.textContent()).isNull();
        }

        @Test
        @DisplayName("template params carry the username and the verification link, nothing else")
        void templateParams() {
            givenOwner(TO, USERNAME);

            sender.sendEmailVerification(TO, TOKEN);

            assertThat(sentPayload().params())
                    .containsOnlyKeys("USER_NAME", "VERIFICATION_LINK")
                    .containsEntry("USER_NAME", USERNAME)
                    .containsEntry("VERIFICATION_LINK", BASE_URL + "/user/emails/verify?token=" + TOKEN);
        }

        @Test
        @DisplayName("returns Brevo's response untouched")
        void returnsBrevoResponse() {
            givenOwner(TO, USERNAME);
            CreateSmtpEmail expected = new CreateSmtpEmail("<202611171500.1@smtp-relay.test>", null);
            when(brevoService.sendTransacEmail(any(SendSmtpEmail.class))).thenReturn(expected);

            CreateSmtpEmail result = sender.sendEmailVerification(TO, TOKEN);

            assertThat(result).isSameAs(expected);
        }

        @Test
        @DisplayName("unknown address: IllegalArgumentException and Brevo is never called")
        void unknownAddress() {
            when(userEmailRepository.findByEmail(TO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sender.sendEmailVerification(TO, TOKEN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(TO);

            verifyNoInteractions(brevoService);
        }

        @Test
        @DisplayName("a Brevo failure propagates as is: it is not swallowed")
        void brevoFailurePropagates() {
            givenOwner(TO, USERNAME);
            RuntimeException boom = new RuntimeException("brevo down");
            when(brevoService.sendTransacEmail(any(SendSmtpEmail.class))).thenThrow(boom);

            assertThatThrownBy(() -> sender.sendEmailVerification(TO, TOKEN))
                    .isSameAs(boom);
        }
    }
}
