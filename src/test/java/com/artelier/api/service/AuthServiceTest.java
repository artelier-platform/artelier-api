package com.artelier.api.service;

import com.artelier.api.dto.request.LoginRequest;
import com.artelier.api.dto.request.RegisterRequest;
import com.artelier.api.dto.response.AuthResponse;
import com.artelier.api.dto.response.IssuedSession;
import com.artelier.api.dto.response.UserProfileResponse;
import com.artelier.api.entity.User;
import com.artelier.api.entity.UserSession;
import com.artelier.api.enums.Role;
import com.artelier.api.enums.SessionRevocationReason;
import com.artelier.api.enums.UserStatus;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.mapper.UserMapper;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.impl.AuthServiceImpl;
import com.artelier.api.service.impl.LoginAttemptServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    private static final String PASSWORD = "StrongPass123";
    private static final long EXPIRATION_MS = 900_000L;

    @Mock private UserRepository userRepository;
    @Mock private UserEmailService userEmailService;
    @Mock private UserSessionService userSessionService;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LoginAttemptServiceImpl loginAttemptService;
    @Mock private UserMapper userMapper;

    @InjectMocks private AuthServiceImpl authService;

    // =========================================================
    // Fixtures
    // =========================================================

    private static User userWithStatus(UserStatus status) {
        return User.builder()
                .id(UUID.randomUUID())
                .username("Geronimo01")
                .passwordHash("hashed")
                .firstName("Geronimo")
                .lastName("Rodriguez")
                .phoneCountryCode("+57")
                .phoneNumber("3001234567")
                .role(Role.BUYER)
                .status(status)
                .build();
    }

    private static UserProfileResponse profileOf(User u) {
        return new UserProfileResponse(
                u.getId(), u.getUsername(), u.getFirstName(), u.getLastName(),
                u.getPhoneCountryCode(), u.getPhoneNumber(), null, null,
                u.getStatus(), u.getRole(), List.of(), null);
    }

    private static UserSession sessionOf(User user) {
        return UserSession.builder()
                .id(UUID.randomUUID())
                .user(user)
                .refreshTokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private static IssuedSession issuedFor(User user, String refreshToken) {
        return new IssuedSession(sessionOf(user), refreshToken);
    }

    private static RegisterRequest registerRequest(String username) {
        return new RegisterRequest(username, PASSWORD, "Geronimo", "Rodriguez",
                "geronimo@example.com", "+57", "3001234567");
    }

    /** Stubs what turns an issued session into an AuthResponse. */
    private void stubTokens(User user, IssuedSession issued) {
        when(jwtUtil.generateAccessToken(user.getId(), issued.session().getId(), user.getRole().name()))
                .thenReturn("access-token");
        when(jwtUtil.getExpirationMs()).thenReturn(EXPIRATION_MS);
        when(userMapper.toProfileResponse(user)).thenReturn(profileOf(user));
    }

    private static void assertStatus(Runnable action, HttpStatus expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ArtelierException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(expected));
    }

    // =========================================================
    // register
    // =========================================================

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("creates a PENDING_VERIFICATION BUYER, adds the email and opens a session, in that order")
        void createsUserEmailAndSession() {
            UUID userId = UUID.randomUUID();
            UserSession session = UserSession.builder().id(UUID.randomUUID())
                    .refreshTokenHash("hash").expiresAt(Instant.now().plusSeconds(3600)).build();

            when(userRepository.existsByUsernameIgnoreCase("Geronimo01")).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded");
            when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(userId);
                return u;
            });
            when(userSessionService.create(any(User.class)))
                    .thenReturn(new IssuedSession(session, "refresh-1"));
            when(jwtUtil.generateAccessToken(userId, session.getId(), "BUYER")).thenReturn("access-token");
            when(jwtUtil.getExpirationMs()).thenReturn(EXPIRATION_MS);
            when(userMapper.toProfileResponse(any(User.class)))
                    .thenAnswer(inv -> profileOf(inv.getArgument(0)));

            AuthResponse response = authService.register(registerRequest("Geronimo01"));

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).saveAndFlush(saved.capture());
            User user = saved.getValue();
            assertThat(user.getUsername()).isEqualTo("Geronimo01"); // as typed
            assertThat(user.getPasswordHash()).isEqualTo("encoded");
            assertThat(user.getFirstName()).isEqualTo("Geronimo");
            assertThat(user.getLastName()).isEqualTo("Rodriguez");
            assertThat(user.getPhoneCountryCode()).isEqualTo("+57");
            assertThat(user.getPhoneNumber()).isEqualTo("3001234567");
            assertThat(user.getRole()).isEqualTo(Role.BUYER);
            assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-1");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(900L);
            assertThat(response.user()).isEqualTo(profileOf(user));

            // The FK needs the user first; the session needs the user too.
            InOrder order = inOrder(userRepository, userEmailService, userSessionService);
            order.verify(userRepository).saveAndFlush(any(User.class));
            order.verify(userEmailService).addEmail(userId, "geronimo@example.com");
            order.verify(userSessionService).create(user);
        }

        @Test
        @DisplayName("409 – username taken, ignoring case (MimiRandomS exists, mimirandoms is rejected)")
        void duplicateUsernameIgnoringCase() {
            when(userRepository.existsByUsernameIgnoreCase("mimirandoms")).thenReturn(true);

            assertStatus(() -> authService.register(registerRequest("mimirandoms")),
                    HttpStatus.CONFLICT);

            verify(userRepository, never()).saveAndFlush(any());
            verifyNoInteractions(passwordEncoder, userEmailService, userSessionService, jwtUtil);
        }

        @Test
        @DisplayName("409 – concurrent registration: the DB unique constraint wins the race")
        void raceOnUniqueConstraint() {
            when(userRepository.existsByUsernameIgnoreCase("Geronimo01")).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(userRepository.saveAndFlush(any(User.class)))
                    .thenThrow(new DataIntegrityViolationException("uk_users_username_lower"));

            assertStatus(() -> authService.register(registerRequest("Geronimo01")),
                    HttpStatus.CONFLICT);

            verifyNoInteractions(userEmailService, userSessionService, jwtUtil);
        }

        @Test
        @DisplayName("409 – email already used: no session and no tokens are issued")
        void emailAlreadyUsed() {
            UUID userId = UUID.randomUUID();
            when(userRepository.existsByUsernameIgnoreCase("Geronimo01")).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(userId);
                return u;
            });
            doThrow(ArtelierException.conflict("Email already in use"))
                    .when(userEmailService).addEmail(userId, "geronimo@example.com");

            assertStatus(() -> authService.register(registerRequest("Geronimo01")),
                    HttpStatus.CONFLICT);

            verifyNoInteractions(userSessionService, jwtUtil);
        }
    }

    // =========================================================
    // login
    // =========================================================

    @Nested
    @DisplayName("login")
    class Login {

        @ParameterizedTest(name = "by username, account {0}")
        @EnumSource(value = UserStatus.class, names = {"ACTIVE", "PENDING_VERIFICATION"})
        @DisplayName("accounts that may sign in: ACTIVE and PENDING_VERIFICATION")
        void loginByUsername(UserStatus status) {
            User user = userWithStatus(status);
            IssuedSession issued = issuedFor(user, "refresh-1");

            // The attempts key is lowercase so MimiRandomS / mimirandoms share one bucket.
            when(loginAttemptService.tryConsume("geronimo01")).thenReturn(true);
            when(userRepository.findByUsernameIgnoreCase("Geronimo01")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, "hashed")).thenReturn(true);
            when(userSessionService.create(user)).thenReturn(issued);
            stubTokens(user, issued);

            AuthResponse response = authService.login(new LoginRequest("Geronimo01", PASSWORD));

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-1");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(900L);
            assertThat(response.user()).isEqualTo(profileOf(user));
            verify(loginAttemptService).reset("geronimo01");
        }

        @Test
        @DisplayName("by verified email (identifier contains '@'), normalized to lowercase")
        void loginByVerifiedEmail() {
            User user = userWithStatus(UserStatus.ACTIVE);
            IssuedSession issued = issuedFor(user, "refresh-1");

            when(loginAttemptService.tryConsume("geronimo@example.com")).thenReturn(true);
            when(userEmailService.findVerifiedOwner("geronimo@example.com"))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, "hashed")).thenReturn(true);
            when(userSessionService.create(user)).thenReturn(issued);
            stubTokens(user, issued);

            AuthResponse response = authService.login(
                    new LoginRequest("Geronimo@Example.COM", PASSWORD));

            assertThat(response.accessToken()).isEqualTo("access-token");
            verify(userRepository, never()).findByUsernameIgnoreCase(any());
            verify(loginAttemptService).reset("geronimo@example.com");
        }

        @Test
        @DisplayName("401 – pending or unknown email gets the same error as a wrong password")
        void unverifiedOrUnknownEmail() {
            when(loginAttemptService.tryConsume("pending@example.com")).thenReturn(true);
            when(userEmailService.findVerifiedOwner("pending@example.com"))
                    .thenReturn(Optional.empty());

            assertStatus(() -> authService.login(new LoginRequest("pending@example.com", PASSWORD)),
                    HttpStatus.UNAUTHORIZED);

            verifyNoInteractions(userSessionService, jwtUtil);
        }

        @Test
        @DisplayName("401 – unknown username gets the same error as a wrong password")
        void unknownUsername() {
            when(loginAttemptService.tryConsume("ghost")).thenReturn(true);
            when(userRepository.findByUsernameIgnoreCase("ghost")).thenReturn(Optional.empty());

            assertStatus(() -> authService.login(new LoginRequest("ghost", PASSWORD)),
                    HttpStatus.UNAUTHORIZED);

            verifyNoInteractions(userSessionService, jwtUtil);
        }

        @Test
        @DisplayName("401 – wrong password: no session and the attempts are not reset")
        void wrongPassword() {
            User user = userWithStatus(UserStatus.ACTIVE);
            when(loginAttemptService.tryConsume("geronimo01")).thenReturn(true);
            when(userRepository.findByUsernameIgnoreCase("Geronimo01")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

            assertStatus(() -> authService.login(new LoginRequest("Geronimo01", "wrong-password")),
                    HttpStatus.UNAUTHORIZED);

            verify(loginAttemptService, never()).reset(any());
            verifyNoInteractions(userSessionService, jwtUtil);
        }

        @Test
        @DisplayName("403 – suspended account with the correct password")
        void suspendedWithCorrectPassword() {
            User user = userWithStatus(UserStatus.SUSPENDED);
            when(loginAttemptService.tryConsume("geronimo01")).thenReturn(true);
            when(userRepository.findByUsernameIgnoreCase("Geronimo01")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, "hashed")).thenReturn(true);

            assertStatus(() -> authService.login(new LoginRequest("Geronimo01", PASSWORD)),
                    HttpStatus.FORBIDDEN);

            verifyNoInteractions(userSessionService, jwtUtil);
        }

        @Test
        @DisplayName("401 – suspended account with a wrong password does not reveal the suspension")
        void suspendedWithWrongPassword() {
            User user = userWithStatus(UserStatus.SUSPENDED);
            when(loginAttemptService.tryConsume("geronimo01")).thenReturn(true);
            when(userRepository.findByUsernameIgnoreCase("Geronimo01")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

            assertStatus(() -> authService.login(new LoginRequest("Geronimo01", "wrong-password")),
                    HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("400 – rate limit exceeded: nothing else is touched")
        void rateLimited() {
            when(loginAttemptService.tryConsume("geronimo01")).thenReturn(false);

            assertStatus(() -> authService.login(new LoginRequest("Geronimo01", PASSWORD)),
                    HttpStatus.BAD_REQUEST);

            verifyNoInteractions(userRepository, userEmailService, passwordEncoder,
                    userSessionService, jwtUtil);
        }
    }

    // =========================================================
    // refresh
    // =========================================================

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("rotates the session and returns a new token pair")
        void rotatesSession() {
            User user = userWithStatus(UserStatus.ACTIVE);
            IssuedSession rotated = issuedFor(user, "new-refresh");
            when(userSessionService.rotate("old-refresh")).thenReturn(rotated);
            stubTokens(user, rotated);

            AuthResponse response = authService.refresh("old-refresh");

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("new-refresh");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(900L);
            assertThat(response.user()).isEqualTo(profileOf(user));
        }

        @Test
        @DisplayName("403 – suspended account does not get an access token")
        void suspendedAccount() {
            User user = userWithStatus(UserStatus.SUSPENDED);
            when(userSessionService.rotate("old-refresh")).thenReturn(issuedFor(user, "new-refresh"));

            assertStatus(() -> authService.refresh("old-refresh"), HttpStatus.FORBIDDEN);

            verifyNoInteractions(jwtUtil);
        }

        @Test
        @DisplayName("401 – invalid, expired, revoked or reused token is propagated")
        void invalidToken() {
            when(userSessionService.rotate("bad"))
                    .thenThrow(ArtelierException.unauthorized("Invalid refresh token"));

            assertStatus(() -> authService.refresh("bad"), HttpStatus.UNAUTHORIZED);

            verifyNoInteractions(jwtUtil);
        }
    }

    // =========================================================
    // logout
    // =========================================================

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("revokes only the session of that refresh token (USER_LOGOUT)")
        void revokesSession() {
            authService.logout("refresh-token");

            verify(userSessionService).revoke("refresh-token", SessionRevocationReason.USER_LOGOUT);
        }

        @Test
        @DisplayName("401 – unknown token is propagated")
        void invalidToken() {
            doThrow(ArtelierException.unauthorized("Invalid refresh token"))
                    .when(userSessionService).revoke("bad", SessionRevocationReason.USER_LOGOUT);

            assertStatus(() -> authService.logout("bad"), HttpStatus.UNAUTHORIZED);
        }
    }
}