package com.artelier.api.auth.service;

import com.artelier.api.auth.dto.IssuedSession;
import com.artelier.api.user.entity.User;
import com.artelier.api.auth.entity.UserSession;
import com.artelier.api.auth.enums.SessionRevocationReason;
import com.artelier.api.shared.exception.ArtelierException;
import com.artelier.api.user.repository.UserRepository;
import com.artelier.api.auth.repository.UserSessionRepository;
import com.artelier.api.auth.security.UserJwtService;
import com.artelier.api.auth.security.RefreshTokenHasher;
import com.artelier.api.auth.service.impl.UserSessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.artelier.api.auth.security.JwtFilter;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSessionService")
class UserSessionServiceTest {

    private static final String OLD_TOKEN = "old-refresh-token";
    private static final String OLD_HASH = "hash:" + OLD_TOKEN;
    private static final int MIN_TOKEN_LENGTH = 43; // 32 random bytes in base64url

    @Mock private UserSessionRepository sessionRepository;
    @Mock private RefreshTokenHasher hasher;
    @Mock private UserRepository userRepository;
    @Mock private UserJwtService UserJwtService;
    @InjectMocks private UserSessionServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshExpirationDays", 7L);

        lenient().when(hasher.hash(anyString()))
                .thenAnswer(invocation ->
                        "hash:" + invocation.getArgument(0)
                );

        lenient().when(UserJwtService.generateAccessToken(
                any(UUID.class),
                any(UUID.class),
                any()
        )).thenAnswer(invocation ->
                UUID.randomUUID().toString() + UUID.randomUUID()
        );

        user = User.builder()
                .id(UUID.randomUUID())
                .username("Geronimo01")
                .build();
    }

    // =========================================================
    // Fixtures
    // =========================================================

    private UserSession sessionWith(Instant expiresAt, Instant revokedAt,
                                    SessionRevocationReason reason) {
        return UserSession.builder()
                .id(UUID.randomUUID())
                .user(user)
                .refreshTokenHash(OLD_HASH)
                .expiresAt(expiresAt)
                .revokedAt(revokedAt)
                .revocationReason(reason)
                .build();
    }

    private UserSession activeSession() {
        return sessionWith(Instant.now().plus(1, ChronoUnit.DAYS), null, null);
    }

    private static void assertUnauthorized(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ArtelierException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // =========================================================
    // create
    // =========================================================

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("persists only the hash of the refresh token, valid for the configured days")
        void persistsHashOnly() {
            when(sessionRepository.save(any(UserSession.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Instant before = Instant.now();
            IssuedSession issued = service.create(user);
            Instant after = Instant.now();

            ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
            verify(sessionRepository).save(captor.capture());
            UserSession saved = captor.getValue();

            assertThat(issued.session()).isSameAs(saved);
            assertThat(saved.getUser()).isSameAs(user);
            assertThat(saved.getRefreshTokenHash())
                    .isEqualTo("hash:" + issued.refreshToken())
                    .isNotEqualTo(issued.refreshToken());
            assertThat(saved.getExpiresAt())
                    .isBetween(before.plus(7, ChronoUnit.DAYS), after.plus(7, ChronoUnit.DAYS));
            assertThat(saved.getRevokedAt()).isNull();
            assertThat(saved.isValid()).isTrue();
        }

        @Test
        @DisplayName("the refresh token is long and different every time")
        void tokenIsUnpredictable() {
            when(sessionRepository.save(any(UserSession.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            IssuedSession first = service.create(user);
            IssuedSession second = service.create(user);

            assertThat(first.refreshToken()).hasSizeGreaterThanOrEqualTo(MIN_TOKEN_LENGTH);
            assertThat(second.refreshToken()).hasSizeGreaterThanOrEqualTo(MIN_TOKEN_LENGTH);
            assertThat(first.refreshToken()).isNotEqualTo(second.refreshToken());
        }

        @Test
        @DisplayName("several sessions of the same user are independent: creating one never revokes another")
        void sessionsAreIndependent() {
            when(sessionRepository.save(any(UserSession.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.create(user);
            service.create(user);

            ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
            verify(sessionRepository, times(2)).save(captor.capture());
            assertThat(captor.getAllValues())
                    .extracting(UserSession::getRefreshTokenHash)
                    .doesNotHaveDuplicates();
            assertThat(captor.getAllValues()).allMatch(UserSession::isValid);
            verify(sessionRepository, never()).delete(any());
        }
    }

    // =========================================================
    // rotate
    // =========================================================

    @Nested
    @DisplayName("rotate")
    class Rotate {

        @Test
        @DisplayName("looks the session up by hash, swaps the hash atomically and returns the new token")
        void rotatesSession() {
            UserSession session = activeSession();
            when(sessionRepository.findByRefreshTokenHash(OLD_HASH)).thenReturn(Optional.of(session));
            when(sessionRepository.rotateRefreshTokenHash(eq(OLD_HASH), anyString(), any(Instant.class)))
                    .thenReturn(1);

            Instant before = Instant.now();
            IssuedSession issued = service.rotate(OLD_TOKEN);
            Instant after = Instant.now();

            assertThat(issued.session()).isSameAs(session);
            assertThat(issued.refreshToken())
                    .isNotEqualTo(OLD_TOKEN)
                    .hasSizeGreaterThanOrEqualTo(MIN_TOKEN_LENGTH);

            ArgumentCaptor<Instant> now = ArgumentCaptor.forClass(Instant.class);
            verify(sessionRepository).rotateRefreshTokenHash(
                    eq(OLD_HASH), eq("hash:" + issued.refreshToken()), now.capture());
            assertThat(now.getValue()).isBetween(before, after);
        }

        @Test
        @DisplayName("401 – unknown token (this is also what a reused, already rotated token looks like)")
        void unknownToken() {
            when(sessionRepository.findByRefreshTokenHash(OLD_HASH)).thenReturn(Optional.empty());

            assertUnauthorized(() -> service.rotate(OLD_TOKEN));

            verify(sessionRepository, never()).rotateRefreshTokenHash(any(), any(), any());
        }

        @Test
        @DisplayName("401 – expired session")
        void expiredSession() {
            UserSession expired = sessionWith(Instant.now().minusSeconds(10), null, null);
            when(sessionRepository.findByRefreshTokenHash(OLD_HASH)).thenReturn(Optional.of(expired));

            assertUnauthorized(() -> service.rotate(OLD_TOKEN));

            verify(sessionRepository, never()).rotateRefreshTokenHash(any(), any(), any());
        }

        @Test
        @DisplayName("401 – revoked session")
        void revokedSession() {
            UserSession revoked = sessionWith(Instant.now().plus(1, ChronoUnit.DAYS),
                    Instant.now().minusSeconds(10), SessionRevocationReason.USER_LOGOUT);
            when(sessionRepository.findByRefreshTokenHash(OLD_HASH)).thenReturn(Optional.of(revoked));

            assertUnauthorized(() -> service.rotate(OLD_TOKEN));

            verify(sessionRepository, never()).rotateRefreshTokenHash(any(), any(), any());
        }

        @Test
        @DisplayName("401 – a concurrent request consumed the token first (0 rows updated)")
        void lostTheRace() {
            when(sessionRepository.findByRefreshTokenHash(OLD_HASH))
                    .thenReturn(Optional.of(activeSession()));
            when(sessionRepository.rotateRefreshTokenHash(eq(OLD_HASH), anyString(), any(Instant.class)))
                    .thenReturn(0);

            assertUnauthorized(() -> service.rotate(OLD_TOKEN));
        }
    }

    // =========================================================
    // revoke
    // =========================================================

    @Nested
    @DisplayName("revoke")
    class Revoke {

        @ParameterizedTest(name = "reason {0}")
        @EnumSource(value = SessionRevocationReason.class,
                names = {"USER_LOGOUT", "PASSWORD_CHANGED", "ADMIN_ACTION"})
        @DisplayName("revokes the session with the given reason")
        void revokesSession(SessionRevocationReason reason) {
            UserSession session = activeSession();
            when(sessionRepository.findByRefreshTokenHash(OLD_HASH)).thenReturn(Optional.of(session));

            Instant before = Instant.now();
            service.revoke(OLD_TOKEN, reason);
            Instant after = Instant.now();

            assertThat(session.isRevoked()).isTrue();
            assertThat(session.isValid()).isFalse();
            assertThat(session.getRevocationReason()).isEqualTo(reason);
            assertThat(session.getRevokedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("401 – unknown token")
        void unknownToken() {
            when(sessionRepository.findByRefreshTokenHash(OLD_HASH)).thenReturn(Optional.empty());

            assertUnauthorized(() -> service.revoke(OLD_TOKEN, SessionRevocationReason.USER_LOGOUT));
        }

        @Test
        @DisplayName("revoking twice is idempotent: the original reason and timestamp are kept")
        void idempotent() {
            Instant revokedAt = Instant.now().minusSeconds(60);
            UserSession session = sessionWith(Instant.now().plus(1, ChronoUnit.DAYS),
                    revokedAt, SessionRevocationReason.USER_LOGOUT);
            when(sessionRepository.findByRefreshTokenHash(OLD_HASH)).thenReturn(Optional.of(session));

            service.revoke(OLD_TOKEN, SessionRevocationReason.ADMIN_ACTION);

            assertThat(session.getRevokedAt()).isEqualTo(revokedAt);
            assertThat(session.getRevocationReason()).isEqualTo(SessionRevocationReason.USER_LOGOUT);
        }
    }

    // =========================================================
    // isActive (used by JwtFilter on every request)
    // =========================================================

    @Nested
    @DisplayName("isActive")
    class IsActive {

        @Test
        @DisplayName("true: the session exists, belongs to that user, is not revoked and not expired")
        void returnsTrueForActiveSession() {
            UserSession session = activeSession();
            when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

            assertThat(service.isActive(session.getId(), user.getId())).isTrue();
        }

        @Test
        @DisplayName("false: unknown session")
        void unknownSession() {
            UUID id = UUID.randomUUID();
            when(sessionRepository.findById(id)).thenReturn(Optional.empty());

            assertThat(service.isActive(id, user.getId())).isFalse();
        }

        @Test
        @DisplayName("false: the session belongs to another user")
        void sessionOfAnotherUser() {
            UserSession session = activeSession();
            when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

            assertThat(service.isActive(session.getId(), UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("false: expired session")
        void expiredSession() {
            UserSession expired = sessionWith(Instant.now().minusSeconds(10), null, null);
            when(sessionRepository.findById(expired.getId())).thenReturn(Optional.of(expired));

            assertThat(service.isActive(expired.getId(), user.getId())).isFalse();
        }

        @Test
        @DisplayName("false: revoked session")
        void revokedSession() {
            UserSession revoked = sessionWith(Instant.now().plus(1, ChronoUnit.DAYS),
                    Instant.now().minusSeconds(10), SessionRevocationReason.USER_LOGOUT);
            when(sessionRepository.findById(revoked.getId())).thenReturn(Optional.of(revoked));

            assertThat(service.isActive(revoked.getId(), user.getId())).isFalse();
        }
    }
}