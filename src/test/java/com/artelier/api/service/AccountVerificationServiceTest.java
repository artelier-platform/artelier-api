package com.artelier.api.service;

import com.artelier.api.entity.User;
import com.artelier.api.entity.UserEmail;
import com.artelier.api.enums.Role;
import com.artelier.api.enums.UserStatus;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.repository.UserEmailRepository;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.service.impl.AccountVerificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountVerificationService")
class AccountVerificationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserEmailRepository emailRepository;

    @InjectMocks private AccountVerificationServiceImpl service;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).username("Geronimo01").role(Role.BUYER)
                .status(UserStatus.PENDING_VERIFICATION).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    private UserEmail email(boolean verified) {
        return UserEmail.builder().id(UUID.randomUUID()).email(UUID.randomUUID() + "@example.com")
                .user(user).isPrimary(verified).isVerified(verified)
                .verifiedAt(verified ? Instant.now() : null).build();
    }

    /** Lenient: an implementation may check the phone first and skip the emails query. */
    private void givenState(boolean emailVerified, boolean phoneVerified) {
        List<UserEmail> emails = emailVerified
                ? List.of(email(true), email(false))
                : List.of(email(false));
        lenient().when(emailRepository.findByUserId(userId)).thenReturn(emails);
        user.setPhoneVerifiedAt(phoneVerified ? Instant.now() : null);
    }

    @ParameterizedTest(name = "email verified={0}, phone verified={1} → {2}")
    @CsvSource({
            "true,  true,  ACTIVE",
            "true,  false, PENDING_VERIFICATION",
            "false, true,  PENDING_VERIFICATION",
            "false, false, PENDING_VERIFICATION"
    })
    @DisplayName("a pending account becomes ACTIVE only with a verified email AND a verified phone")
    void activationMatrix(boolean emailVerified, boolean phoneVerified, UserStatus expected) {
        givenState(emailVerified, phoneVerified);

        UserStatus result = service.activateIfEligible(userId);

        assertThat(result).isEqualTo(expected);
        assertThat(user.getStatus()).isEqualTo(expected);
    }

    @Test
    @DisplayName("a suspended account is never activated by the verification flow")
    void suspendedStaysSuspended() {
        user.setStatus(UserStatus.SUSPENDED);
        givenState(true, true);

        UserStatus result = service.activateIfEligible(userId);

        assertThat(result).isEqualTo(UserStatus.SUSPENDED);
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }

    @Test
    @DisplayName("an ACTIVE account stays ACTIVE (idempotent)")
    void activeStaysActive() {
        user.setStatus(UserStatus.ACTIVE);
        givenState(true, true);

        assertThat(service.activateIfEligible(userId)).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("404 – unknown user")
    void unknownUser() {
        UUID other = UUID.randomUUID();
        when(userRepository.findById(other)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activateIfEligible(other))
                .isInstanceOfSatisfying(ArtelierException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}