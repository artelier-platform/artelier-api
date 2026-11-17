package com.artelier.api.user.service;

import com.artelier.api.user.dto.UserEmailResponse;
import com.artelier.api.user.entity.User;
import com.artelier.api.user.entity.UserEmail;
import com.artelier.api.user.enums.Role;
import com.artelier.api.user.enums.UserStatus;
import com.artelier.api.shared.exception.ArtelierException;
import com.artelier.api.integration.mail.Service.VerificationMailSender;
import com.artelier.api.user.mapper.UserEmailMapper;
import com.artelier.api.user.mapper.UserMapper;
import com.artelier.api.user.repository.UserEmailRepository;
import com.artelier.api.user.repository.UserRepository;
import com.artelier.api.user.service.impl.UserEmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.artelier.api.verification.service.AccountVerificationService;
import com.artelier.api.verification.service.EmailVerificationTokens;

/**
 * Contract: 409 = operation not valid in the current state; 404 also hides emails of other users.
 * Contract: the current primary is found with findByUserId; removal uses emailRepository.delete.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserEmailService")
class UserEmailServiceTest {

    @Mock private UserEmailRepository emailRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserEmailMapper userEmailMapper;
    @Mock private EmailVerificationTokens tokens;
    @Mock private VerificationMailSender mailSender;
    @Mock private AccountVerificationService accountVerificationService;

    @InjectMocks private UserEmailServiceImpl service;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).username("Geronimo01")
                .role(Role.BUYER).status(UserStatus.ACTIVE).build();
    }

    // =========================================================
    // Fixtures
    // =========================================================

    private UserEmail email(String address, boolean primary, boolean verified) {
        return UserEmail.builder()
                .id(UUID.randomUUID())
                .email(address)
                .user(user)
                .isPrimary(primary)
                .isVerified(verified)
                .verifiedAt(verified ? Instant.now().minusSeconds(3600) : null)
                .build();
    }

    private static UserEmailResponse responseOf(UserEmail e) {
        return new UserEmailResponse(e.getId(), e.getEmail(), e.isPrimary(),
                e.isVerified(), e.getVerifiedAt(), null);
    }

    private void stubUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    private void stubMapper() {
        when(userEmailMapper.toResponse(any(UserEmail.class)))
                .thenAnswer(inv -> responseOf(inv.getArgument(0)));
    }

    private static void assertStatus(Runnable action, HttpStatus expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ArtelierException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(expected));
    }

    // =========================================================
    // addEmail
    // =========================================================

    @Nested
    @DisplayName("addEmail")
    class AddEmail {

        @Test
        @DisplayName("saves a pending, non-primary email and sends the verification token")
        void addsPendingEmail() {
            stubUser();
            UUID emailId = UUID.randomUUID();
            when(emailRepository.existsByEmail("second@example.com")).thenReturn(false);
            when(emailRepository.countByUserId(userId)).thenReturn(1L);
            when(emailRepository.saveAndFlush(any(UserEmail.class))).thenAnswer(inv -> {
                UserEmail e = inv.getArgument(0);
                e.setId(emailId);
                return e;
            });
            when(tokens.issue(emailId)).thenReturn("verification-token");
            stubMapper();

            UserEmailResponse response = service.addEmail(userId, "second@example.com");

            ArgumentCaptor<UserEmail> captor = ArgumentCaptor.forClass(UserEmail.class);
            verify(emailRepository).saveAndFlush(captor.capture());
            UserEmail saved = captor.getValue();
            assertThat(saved.getEmail()).isEqualTo("second@example.com");
            assertThat(saved.getUser()).isSameAs(user);
            assertThat(saved.isPrimary()).isFalse();
            assertThat(saved.isVerified()).isFalse();
            assertThat(saved.getVerifiedAt()).isNull();
            assertThat(response.email()).isEqualTo("second@example.com");
            assertThat(response.isVerified()).isFalse();

            // The token needs the persisted id; the mail goes out last.
            InOrder order = inOrder(emailRepository, tokens, mailSender);
            order.verify(emailRepository).saveAndFlush(saved);
            order.verify(tokens).issue(emailId);
            order.verify(mailSender).sendEmailVerification("second@example.com", "verification-token");
        }

        @Test
        @DisplayName("normalizes the address (trim + lowercase) before checking and saving")
        void normalizesEmail() {
            stubUser();
            when(emailRepository.existsByEmail("second@example.com")).thenReturn(false);
            when(emailRepository.saveAndFlush(any(UserEmail.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(tokens.issue(any())).thenReturn("t");
            stubMapper();

            service.addEmail(userId, "  Second@Example.COM ");

            ArgumentCaptor<UserEmail> captor = ArgumentCaptor.forClass(UserEmail.class);
            verify(emailRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo("second@example.com");
        }

        @ParameterizedTest(name = "{0} existing emails: accepted")
        @ValueSource(longs = {0, 1, 2})
        @DisplayName("up to three emails per account")
        void acceptsUpToThree(long existing) {
            stubUser();
            when(emailRepository.countByUserId(userId)).thenReturn(existing);
            when(emailRepository.saveAndFlush(any(UserEmail.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(tokens.issue(any())).thenReturn("t");
            stubMapper();

            assertThatCode(() -> service.addEmail(userId, "new@example.com"))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "{0} existing emails: rejected")
        @ValueSource(longs = {3, 4})
        @DisplayName("409 – the fourth email is rejected")
        void rejectsFourth(long existing) {
            stubUser();
            when(emailRepository.countByUserId(userId)).thenReturn(existing);

            assertStatus(() -> service.addEmail(userId, "new@example.com"), HttpStatus.CONFLICT);

            verify(emailRepository, never()).saveAndFlush(any());
            verifyNoInteractions(mailSender);
        }

        @Test
        @DisplayName("409 – the email already belongs to any account (globally unique)")
        void duplicateEmail() {
            stubUser();
            when(emailRepository.existsByEmail("taken@example.com")).thenReturn(true);

            assertStatus(() -> service.addEmail(userId, "taken@example.com"), HttpStatus.CONFLICT);

            verify(emailRepository, never()).saveAndFlush(any());
            verifyNoInteractions(mailSender);
        }

        @Test
        @DisplayName("409 – concurrent insert: the DB unique constraint wins the race")
        void raceOnUniqueConstraint() {
            stubUser();
            when(emailRepository.countByUserId(userId)).thenReturn(1L);
            when(emailRepository.saveAndFlush(any(UserEmail.class)))
                    .thenThrow(new DataIntegrityViolationException("uk_user_emails_email"));

            assertStatus(() -> service.addEmail(userId, "new@example.com"), HttpStatus.CONFLICT);

            verifyNoInteractions(mailSender);
        }

        @Test
        @DisplayName("404 – unknown user")
        void unknownUser() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertStatus(() -> service.addEmail(userId, "new@example.com"), HttpStatus.NOT_FOUND);

            verifyNoInteractions(emailRepository, mailSender);
        }

        @Test
        @DisplayName("403 – suspended account")
        void suspendedAccount() {
            user.setStatus(UserStatus.SUSPENDED);
            stubUser();

            assertStatus(() -> service.addEmail(userId, "new@example.com"), HttpStatus.FORBIDDEN);

            verifyNoInteractions(emailRepository, mailSender);
        }
    }

    // =========================================================
    // verifyEmail
    // =========================================================

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmail {

        @Test
        @DisplayName("the first verified email becomes the primary one (V1 forbids an unverified primary)")
        void firstVerifiedBecomesPrimary() {
            UserEmail pending = email("first@example.com", false, false);
            when(tokens.parse("tok")).thenReturn(pending.getId());
            when(emailRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
            when(emailRepository.findByUserId(userId)).thenReturn(List.of(pending));
            stubMapper();

            Instant before = Instant.now();
            UserEmailResponse response = service.verifyEmail("tok");
            Instant after = Instant.now();

            assertThat(pending.isVerified()).isTrue();
            assertThat(pending.getVerifiedAt()).isBetween(before, after);
            assertThat(pending.isPrimary()).isTrue();
            assertThat(response.isPrimary()).isTrue();
            assertThat(response.isVerified()).isTrue();
            verify(accountVerificationService).activateIfEligible(userId);
        }

        @Test
        @DisplayName("a later verified email does NOT take over the primary")
        void laterVerifiedStaysSecondary() {
            UserEmail primary = email("primary@example.com", true, true);
            UserEmail pending = email("second@example.com", false, false);
            when(tokens.parse("tok")).thenReturn(pending.getId());
            when(emailRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
            when(emailRepository.findByUserId(userId)).thenReturn(List.of(primary, pending));
            stubMapper();

            service.verifyEmail("tok");

            assertThat(pending.isVerified()).isTrue();
            assertThat(pending.isPrimary()).isFalse();
            assertThat(primary.isPrimary()).isTrue();
            verify(accountVerificationService).activateIfEligible(userId);
        }

        @Test
        @DisplayName("verifying an already verified email is a no-op")
        void alreadyVerified() {
            UserEmail already = email("a@example.com", true, true);
            Instant originalVerifiedAt = already.getVerifiedAt();
            when(tokens.parse("tok")).thenReturn(already.getId());
            when(emailRepository.findById(already.getId())).thenReturn(Optional.of(already));
            stubMapper();

            UserEmailResponse response = service.verifyEmail("tok");

            assertThat(already.getVerifiedAt()).isEqualTo(originalVerifiedAt);
            assertThat(response.isVerified()).isTrue();
            verifyNoInteractions(accountVerificationService);
        }

        @Test
        @DisplayName("400 – invalid or expired token: nothing is touched")
        void invalidToken() {
            when(tokens.parse("bad")).thenThrow(ArtelierException.badRequest("Invalid token"));

            assertStatus(() -> service.verifyEmail("bad"), HttpStatus.BAD_REQUEST);

            verifyNoInteractions(emailRepository, accountVerificationService);
        }

        @Test
        @DisplayName("404 – the email was removed after the token was issued")
        void emailNoLongerExists() {
            UUID emailId = UUID.randomUUID();
            when(tokens.parse("tok")).thenReturn(emailId);
            when(emailRepository.findById(emailId)).thenReturn(Optional.empty());

            assertStatus(() -> service.verifyEmail("tok"), HttpStatus.NOT_FOUND);

            verifyNoInteractions(accountVerificationService);
        }
    }

    // =========================================================
    // findVerifiedOwner (used by login)
    // =========================================================

    @Nested
    @DisplayName("findVerifiedOwner")
    class FindVerifiedOwner {

        @Test
        @DisplayName("returns the owner of a verified email, looked up normalized")
        void verified() {
            when(emailRepository.findByEmail("a@example.com"))
                    .thenReturn(Optional.of(email("a@example.com", true, true)));

            assertThat(service.findVerifiedOwner("  A@Example.COM ")).contains(user);
        }

        @Test
        @DisplayName("empty for a pending email: it cannot be used to log in")
        void pending() {
            when(emailRepository.findByEmail("a@example.com"))
                    .thenReturn(Optional.of(email("a@example.com", false, false)));

            assertThat(service.findVerifiedOwner("a@example.com")).isEmpty();
        }

        @Test
        @DisplayName("empty for an unknown email")
        void unknown() {
            when(emailRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThat(service.findVerifiedOwner("ghost@example.com")).isEmpty();
        }
    }

    // =========================================================
    // setPrimary
    // =========================================================

    @Nested
    @DisplayName("setPrimary")
    class SetPrimary {

        @Test
        @DisplayName("promotes a verified email and demotes the old primary: old flushed first")
        void swapsPrimary() {
            stubUser();
            UserEmail oldPrimary = email("old@example.com", true, true);
            UserEmail newPrimary = email("new@example.com", false, true);
            when(emailRepository.findByIdAndUserId(newPrimary.getId(), userId))
                    .thenReturn(Optional.of(newPrimary));
            when(emailRepository.findByUserId(userId)).thenReturn(List.of(oldPrimary, newPrimary));
            when(emailRepository.saveAndFlush(any(UserEmail.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            stubMapper();

            UserEmailResponse response = service.setPrimary(userId, newPrimary.getId());

            assertThat(oldPrimary.isPrimary()).isFalse();
            assertThat(newPrimary.isPrimary()).isTrue();
            assertThat(response.isPrimary()).isTrue();

            // uk_user_emails_primary is a partial unique index: two primaries
            // must never exist at the same time, so the old one is flushed first.
            InOrder order = inOrder(emailRepository);
            order.verify(emailRepository).saveAndFlush(oldPrimary);
            order.verify(emailRepository).saveAndFlush(newPrimary);
        }

        @Test
        @DisplayName("409 – an unverified email cannot be primary")
        void unverifiedCandidate() {
            stubUser();
            UserEmail pending = email("pending@example.com", false, false);
            when(emailRepository.findByIdAndUserId(pending.getId(), userId))
                    .thenReturn(Optional.of(pending));

            assertStatus(() -> service.setPrimary(userId, pending.getId()), HttpStatus.CONFLICT);

            assertThat(pending.isPrimary()).isFalse();
            verify(emailRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("choosing the current primary again is a no-op")
        void alreadyPrimary() {
            stubUser();
            UserEmail primary = email("primary@example.com", true, true);
            when(emailRepository.findByIdAndUserId(primary.getId(), userId))
                    .thenReturn(Optional.of(primary));
            stubMapper();

            UserEmailResponse response = service.setPrimary(userId, primary.getId());

            assertThat(response.isPrimary()).isTrue();
            verify(emailRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("404 – unknown email, or one that belongs to another user")
        void notYours() {
            stubUser();
            UUID emailId = UUID.randomUUID();
            when(emailRepository.findByIdAndUserId(emailId, userId)).thenReturn(Optional.empty());

            assertStatus(() -> service.setPrimary(userId, emailId), HttpStatus.NOT_FOUND);

            verify(emailRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("403 – suspended account")
        void suspendedAccount() {
            user.setStatus(UserStatus.SUSPENDED);
            stubUser();

            assertStatus(() -> service.setPrimary(userId, UUID.randomUUID()), HttpStatus.FORBIDDEN);

            verifyNoInteractions(emailRepository);
        }
    }

    // =========================================================
    // removeEmail
    // =========================================================

    @Nested
    @DisplayName("removeEmail")
    class RemoveEmail {

        @Test
        @DisplayName("removes a secondary email")
        void removesSecondary() {
            stubUser();
            UserEmail secondary = email("second@example.com", false, true);
            when(emailRepository.findByIdAndUserId(secondary.getId(), userId))
                    .thenReturn(Optional.of(secondary));
            when(emailRepository.countByUserId(userId)).thenReturn(2L);

            service.removeEmail(userId, secondary.getId());

            verify(emailRepository).delete(secondary);
        }

        @Test
        @DisplayName("409 – the primary cannot be removed, even with other emails (change it first)")
        void primaryCannotBeRemoved() {
            stubUser();
            UserEmail primary = email("primary@example.com", true, true);
            when(emailRepository.findByIdAndUserId(primary.getId(), userId))
                    .thenReturn(Optional.of(primary));
            lenient().when(emailRepository.countByUserId(userId)).thenReturn(3L);

            assertStatus(() -> service.removeEmail(userId, primary.getId()), HttpStatus.CONFLICT);

            verify(emailRepository, never()).delete(any(UserEmail.class));
        }

        @Test
        @DisplayName("409 – the last verified email cannot be removed (it is always the primary)")
        void lastVerifiedCannotBeRemoved() {
            stubUser();
            UserEmail onlyVerified = email("only@example.com", true, true);
            when(emailRepository.findByIdAndUserId(onlyVerified.getId(), userId))
                    .thenReturn(Optional.of(onlyVerified));
            lenient().when(emailRepository.countByUserId(userId)).thenReturn(2L);

            assertStatus(() -> service.removeEmail(userId, onlyVerified.getId()), HttpStatus.CONFLICT);

            verify(emailRepository, never()).delete(any(UserEmail.class));
        }

        @Test
        @DisplayName("409 – the account must keep at least one email, even a pending one")
        void lastEmailOfAnyKind() {
            stubUser();
            UserEmail onlyPending = email("pending@example.com", false, false);
            when(emailRepository.findByIdAndUserId(onlyPending.getId(), userId))
                    .thenReturn(Optional.of(onlyPending));
            when(emailRepository.countByUserId(userId)).thenReturn(1L);

            assertStatus(() -> service.removeEmail(userId, onlyPending.getId()), HttpStatus.CONFLICT);

            verify(emailRepository, never()).delete(any(UserEmail.class));
        }

        @Test
        @DisplayName("404 – unknown email, or one that belongs to another user")
        void notYours() {
            stubUser();
            UUID emailId = UUID.randomUUID();
            when(emailRepository.findByIdAndUserId(emailId, userId)).thenReturn(Optional.empty());

            assertStatus(() -> service.removeEmail(userId, emailId), HttpStatus.NOT_FOUND);

            verify(emailRepository, never()).delete(any(UserEmail.class));
        }

        @Test
        @DisplayName("403 – suspended account")
        void suspendedAccount() {
            user.setStatus(UserStatus.SUSPENDED);
            stubUser();

            assertStatus(() -> service.removeEmail(userId, UUID.randomUUID()), HttpStatus.FORBIDDEN);

            verifyNoInteractions(emailRepository);
        }
    }
}