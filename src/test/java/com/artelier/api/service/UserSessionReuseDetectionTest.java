package com.artelier.api.service;

import com.artelier.api.dto.response.IssuedSession;
import com.artelier.api.entity.User;
import com.artelier.api.entity.UserSession;
import com.artelier.api.enums.SessionRevocationReason;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.repository.UserSessionRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.security.RefreshTokenHasher;
import com.artelier.api.service.impl.UserSessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Refresh token reuse detection.
 *
 * Model under test: the session keeps its id; only the refresh token hash changes on rotation.
 * The hash that was just replaced is remembered (previousRefreshTokenHash). If that old token
 * shows up again, it was stolen or replayed: every session of the user is revoked and the
 * request fails with 401.
 *
 * ASSUMES: UserSessionRepository#findByPreviousRefreshTokenHash(String) returning Optional<UserSession>.
 * If you name it differently, change it in this file only.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserSessionService - reuse detection")
class UserSessionReuseDetectionTest {

    private static final String OLD_TOKEN = "old-refresh-token";
    private static final String OLD_HASH = "hash:" + OLD_TOKEN;

    @Mock private UserSessionRepository sessionRepository;
    @Mock private RefreshTokenHasher hasher;
    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @InjectMocks private UserSessionServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshExpirationDays", 7L);

        lenient().when(hasher.hash(anyString()))
                .thenAnswer(inv -> "hash:" + inv.getArgument(0));

        lenient().when(jwtUtil.generateAccessToken(any(UUID.class), any(UUID.class), any()))
                .thenAnswer(inv -> UUID.randomUUID().toString() + UUID.randomUUID());

        user = User.builder()
                .id(UUID.randomUUID())
                .username("Geronimo01")
                .build();
    }

    // ---------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------

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

    private static void assertStatus(Runnable action, HttpStatus expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ArtelierException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(expected));
    }

    // ---------------------------------------------------------
    // Reuse of an already rotated token
    // ---------------------------------------------------------

    @Test
    @DisplayName("a token that was already rotated revokes ALL sessions of its owner with SECURITY_ACTION and fails with 401")
    void reusedTokenRevokesAllSessionsOfOwner() {
        UserSession session = activeSession();
        when(sessionRepository.findByRefreshTokenHash(OLD_HASH)).thenReturn(Optional.empty());
        when(sessionRepository.findByPreviousRefreshTokenHash(OLD_HASH)).thenReturn(Optional.of(session));

        Instant before = Instant.now();
        assertStatus(() -> service.rotate(OLD_TOKEN), HttpStatus.UNAUTHORIZED);
        Instant after = Instant.now();

        verify(sessionRepository).revokeAllByUserId(
                eq(user.getId()),
                org.mockito.ArgumentMatchers.argThat(now -> !now.isBefore(before) && !now.isAfter(after)),
                eq(SessionRevocationReason.SECURITY_ACTION));
    }

    @Test
    @DisplayName("a detected reuse never issues a new token nor rotates anything")
    void reusedTokenIssuesNothing() {
        when(sessionRepository.findByRefreshTokenHash(OLD_HASH)).thenReturn(Optional.empty());
        when(sessionRepository.findByPreviousRefreshTokenHash(OLD_HASH)).thenReturn(Optional.of(activeSession()));

        assertStatus(() -> service.rotate(OLD_TOKEN), HttpStatus.UNAUTHORIZED);

        verify(jwtUtil, never()).generateAccessToken(any(), any(), any());
        verify(sessionRepository, never()).rotateRefreshTokenHash(any(), any(), any());
    }

    @Test
    @DisplayName("rotating the same token twice: the first works, the second fails and leaves the sessions revoked")
    void rotatingTwiceFailsAndRevokes() {
        UserSession session = activeSession();
        when(sessionRepository.findByRefreshTokenHash(OLD_HASH))
                .thenReturn(Optional.of(session))   // 1st call: still the current token
                .thenReturn(Optional.empty());      // 2nd call: it is no longer the current token
        when(sessionRepository.rotateRefreshTokenHash(eq(OLD_HASH), anyString(), any(Instant.class)))
                .thenReturn(1);
        when(sessionRepository.findByPreviousRefreshTokenHash(OLD_HASH)).thenReturn(Optional.of(session));

        IssuedSession first = service.rotate(OLD_TOKEN);
        assertThat(first.refreshToken()).isNotEqualTo(OLD_TOKEN);

        assertStatus(() -> service.rotate(OLD_TOKEN), HttpStatus.UNAUTHORIZED);

        verify(sessionRepository).revokeAllByUserId(
                eq(user.getId()), any(Instant.class), eq(SessionRevocationReason.SECURITY_ACTION));
    }

    @Test
    @DisplayName("an unknown token (not current, not previous) is a plain 401 and revokes nobody")
    void unknownTokenDoesNotRevokeAnything() {
        when(sessionRepository.findByRefreshTokenHash(OLD_HASH)).thenReturn(Optional.empty());
        when(sessionRepository.findByPreviousRefreshTokenHash(OLD_HASH)).thenReturn(Optional.empty());

        assertStatus(() -> service.rotate(OLD_TOKEN), HttpStatus.UNAUTHORIZED);

        verify(sessionRepository, never()).revokeAllByUserId(any(), any(), any());
        verify(sessionRepository, never()).rotateRefreshTokenHash(any(), any(), any());
    }

    @Test
    @DisplayName("the current token is never checked against the previous hash (no false positives on a normal rotation)")
    void normalRotationSkipsReuseLookup() {
        when(sessionRepository.findByRefreshTokenHash(OLD_HASH)).thenReturn(Optional.of(activeSession()));
        when(sessionRepository.rotateRefreshTokenHash(eq(OLD_HASH), anyString(), any(Instant.class)))
                .thenReturn(1);

        service.rotate(OLD_TOKEN);

        verify(sessionRepository, never()).findByPreviousRefreshTokenHash(any());
        verify(sessionRepository, never()).revokeAllByUserId(any(), any(), any());
    }

    @Test
    @DisplayName("the new hash differs from the one being replaced, so the old one can be stored as previous")
    void rotationChangesTheHash() {
        when(sessionRepository.findByRefreshTokenHash(OLD_HASH)).thenReturn(Optional.of(activeSession()));
        when(sessionRepository.rotateRefreshTokenHash(eq(OLD_HASH), anyString(), any(Instant.class)))
                .thenReturn(1);

        IssuedSession issued = service.rotate(OLD_TOKEN);

        verify(sessionRepository).rotateRefreshTokenHash(
                eq(OLD_HASH),
                org.mockito.ArgumentMatchers.argThat(newHash ->
                        !newHash.equals(OLD_HASH) && newHash.equals("hash:" + issued.refreshToken())),
                any(Instant.class));
    }

    // ---------------------------------------------------------
    // Other hardening around rotate
    // ---------------------------------------------------------

    @Test
    @DisplayName("lost race (0 rows updated) is 401 and does not mass-revoke by itself")
    void lostRaceIsUnauthorized() {
        when(sessionRepository.findByRefreshTokenHash(OLD_HASH)).thenReturn(Optional.of(activeSession()));
        when(sessionRepository.rotateRefreshTokenHash(eq(OLD_HASH), anyString(), any(Instant.class)))
                .thenReturn(0);

        assertStatus(() -> service.rotate(OLD_TOKEN), HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("a null refresh token is a 400, not a NullPointerException")
    void nullTokenIsBadRequest() {
        assertStatus(() -> service.rotate(null), HttpStatus.BAD_REQUEST);

        verify(sessionRepository, never()).findByRefreshTokenHash(any());
    }

    @Test
    @DisplayName("an expired session is actually marked as revoked with SESSION_EXPIRED, then 401")
    void expiredSessionGetsRevoked() {
        UserSession expired = sessionWith(Instant.now().minusSeconds(10), null, null);
        when(sessionRepository.findByRefreshTokenHash(OLD_HASH)).thenReturn(Optional.of(expired));

        assertStatus(() -> service.rotate(OLD_TOKEN), HttpStatus.UNAUTHORIZED);

        assertThat(expired.isRevoked()).isTrue();
        assertThat(expired.getRevocationReason()).isEqualTo(SessionRevocationReason.SESSION_EXPIRED);
        verify(sessionRepository, never()).revokeAllByUserId(any(), any(), any());
    }

    @Test
    @DisplayName("a revoked session presented again is treated as compromise: revokes ALL sessions of the owner with SECURITY_ACTION and fails with 401")
    void revokedSessionTokenRevokesAllSessions() {
        UserSession revoked = sessionWith(Instant.now().plus(1, ChronoUnit.DAYS),
                Instant.now().minusSeconds(10), SessionRevocationReason.USER_LOGOUT);
        when(sessionRepository.findByRefreshTokenHash(OLD_HASH)).thenReturn(Optional.of(revoked));

        assertStatus(() -> service.rotate(OLD_TOKEN), HttpStatus.UNAUTHORIZED);

        verify(sessionRepository).revokeAllByUserId(
                eq(user.getId()), any(Instant.class), eq(SessionRevocationReason.SECURITY_ACTION));
        verify(sessionRepository, never()).rotateRefreshTokenHash(any(), any(), any());
        verify(jwtUtil, never()).generateAccessToken(any(), any(), any());
    }

    @Test
    @DisplayName("a session that is both revoked and expired still counts as compromise, expiry must not mask it")
    void revokedAndExpiredSessionStillRevokesAll() {
        UserSession session = sessionWith(Instant.now().minusSeconds(60),
                Instant.now().minusSeconds(120), SessionRevocationReason.USER_LOGOUT);
        when(sessionRepository.findByRefreshTokenHash(OLD_HASH)).thenReturn(Optional.of(session));

        assertStatus(() -> service.rotate(OLD_TOKEN), HttpStatus.UNAUTHORIZED);

        verify(sessionRepository).revokeAllByUserId(
                eq(user.getId()), any(Instant.class), eq(SessionRevocationReason.SECURITY_ACTION));
    }

    // ---------------------------------------------------------
    // Transaction semantics (mocks cannot see this, an annotation check can)
    // ---------------------------------------------------------

    @Test
    @DisplayName("rotate must not roll back when it throws ArtelierException, otherwise the revocation is undone")
    void rotateKeepsRevocationOnException() throws NoSuchMethodException {
        Method rotate = UserSessionServiceImpl.class.getMethod("rotate", String.class);

        Transactional tx = rotate.getAnnotation(Transactional.class);

        assertThat(tx)
                .as("rotate must declare its own @Transactional(noRollbackFor = ArtelierException.class)")
                .isNotNull();
        assertThat(tx.noRollbackFor()).contains(ArtelierException.class);
    }
}
