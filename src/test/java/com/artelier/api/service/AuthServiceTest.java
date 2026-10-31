package com.artelier.api.service;

import com.artelier.api.dto.request.LoginRequest;
import com.artelier.api.dto.request.RegisterRequest;
import com.artelier.api.dto.response.AuthResponse;
import com.artelier.api.entity.RefreshToken;
import com.artelier.api.entity.User;
import com.artelier.api.enums.Role;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.impl.AuthServiceImpl;
import com.artelier.api.service.impl.LoginAttemptServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final String EMAIL = "test@mail.com";
    private static final String PASSWORD = "password123";

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private LoginAttemptServiceImpl loginAttemptService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setEmail(EMAIL);
        user.setPasswordHash("hashed");
        user.setRole(Role.BUYER);
        user.setBanned(false);
    }

    // =========================================================
    // Helpers
    // =========================================================

    /** Stubs the collaborators that build the token pair of a new session. */
    private void stubSession() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh");

        when(jwtUtil.generateToken(any(), any())).thenReturn("token");
        when(jwtUtil.getExpirationMs()).thenReturn(900000L);
        when(refreshTokenService.create(any())).thenReturn(refreshToken);
    }

    private static void assertError(HttpStatus status, String message, ArtelierException ex) {
        assertEquals(status, ex.getStatus());
        assertEquals(message, ex.getMessage());
    }

    // =========================================================
    // LOGIN
    // =========================================================

    @Test
    void login_success() {
        when(loginAttemptService.tryConsume(EMAIL)).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, "hashed")).thenReturn(true);
        stubSession();

        AuthResponse response = authService.login(new LoginRequest(EMAIL, PASSWORD));

        assertNotNull(response);
        assertEquals("token", response.token());
        assertEquals("BUYER", response.role());
        assertEquals("refresh", response.refreshToken());
        assertEquals(900L, response.expiresIn());
        verify(jwtUtil).generateToken(EMAIL, "BUYER");
    }

    @Test
    void login_rateLimitExceeded() {
        when(loginAttemptService.tryConsume(EMAIL)).thenReturn(false);

        ArtelierException ex = assertThrows(ArtelierException.class,
                () -> authService.login(new LoginRequest(EMAIL, PASSWORD)));

        assertError(HttpStatus.BAD_REQUEST,
                "Too many failed attempts. Try again in 5 minutes", ex);
        verifyNoInteractions(userRepository);
    }

    @Test
    void login_userNotFound_returnsSameErrorAsWrongPassword() {
        when(loginAttemptService.tryConsume(EMAIL)).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        ArtelierException ex = assertThrows(ArtelierException.class,
                () -> authService.login(new LoginRequest(EMAIL, PASSWORD)));

        assertError(HttpStatus.UNAUTHORIZED, "Invalid credentials", ex);
        verifyNoInteractions(jwtUtil, refreshTokenService);
    }

    @Test
    void login_invalidPassword() {
        when(loginAttemptService.tryConsume(EMAIL)).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        ArtelierException ex = assertThrows(ArtelierException.class,
                () -> authService.login(new LoginRequest(EMAIL, "wrong-password")));

        assertError(HttpStatus.UNAUTHORIZED, "Invalid credentials", ex);
        verifyNoInteractions(jwtUtil, refreshTokenService);
    }

    @Test
    void login_bannedUser_withCorrectPassword_isForbidden() {
        user.setBanned(true);

        when(loginAttemptService.tryConsume(EMAIL)).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, "hashed")).thenReturn(true);

        ArtelierException ex = assertThrows(ArtelierException.class,
                () -> authService.login(new LoginRequest(EMAIL, PASSWORD)));

        assertError(HttpStatus.FORBIDDEN, "User is banned", ex);
        verifyNoInteractions(jwtUtil, refreshTokenService);
    }

    @Test
    void login_bannedUser_withWrongPassword_doesNotRevealTheBan() {
        user.setBanned(true);

        when(loginAttemptService.tryConsume(EMAIL)).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        ArtelierException ex = assertThrows(ArtelierException.class,
                () -> authService.login(new LoginRequest(EMAIL, "wrong-password")));

        assertError(HttpStatus.UNAUTHORIZED, "Invalid credentials", ex);
    }

    // =========================================================
    // REGISTER
    // =========================================================

    @Test
    void register_success() {
        RegisterRequest request =
                new RegisterRequest("new@mail.com", PASSWORD, "John Doe", "+573001234567");

        when(userRepository.existsByEmail("new@mail.com")).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("hashed");
        stubSession();

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("token", response.token());
        assertEquals("BUYER", response.role());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("new@mail.com", saved.getValue().getEmail());
        assertEquals("hashed", saved.getValue().getPasswordHash());
        assertEquals("John Doe", saved.getValue().getFullName());
        assertEquals("+573001234567", saved.getValue().getPhone());
        assertEquals(Role.BUYER, saved.getValue().getRole());
    }

    @Test
    void register_emailExists() {
        RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, "John Doe", null);

        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        ArtelierException ex = assertThrows(ArtelierException.class,
                () -> authService.register(request));

        assertError(HttpStatus.CONFLICT, "Email already in use", ex);
        verify(userRepository, never()).save(any());
    }

    // =========================================================
    // REFRESH
    // =========================================================

    @Test
    void refresh_success() {
        RefreshToken oldToken = new RefreshToken();
        oldToken.setUser(user);
        oldToken.setToken("old");

        RefreshToken newToken = new RefreshToken();
        newToken.setToken("newRefresh");

        when(refreshTokenService.consume("old"))
                .thenReturn(oldToken);

        when(jwtUtil.generateToken(any(), any()))
                .thenReturn("newToken");

        when(jwtUtil.getExpirationMs())
                .thenReturn(900000L);

        when(refreshTokenService.create(user))
                .thenReturn(newToken);

        AuthResponse response = authService.refresh("old");

        assertNotNull(response);
        assertEquals("newToken", response.token());
        assertEquals("newRefresh", response.refreshToken());
        assertEquals(900L, response.expiresIn());

        verify(refreshTokenService).consume("old");
        verify(refreshTokenService).create(user);
        verify(refreshTokenService, never()).revokeByToken(anyString());
    }

    @Test
    void refresh_userBanned() {
        user.setBanned(true);

        RefreshToken token = new RefreshToken();
        token.setUser(user);

        when(refreshTokenService.consume("token"))
                .thenReturn(token);

        ArtelierException ex = assertThrows(
                ArtelierException.class,
                () -> authService.refresh("token")
        );

        assertError(
                HttpStatus.FORBIDDEN,
                "User is banned",
                ex
        );

        verify(refreshTokenService).consume("token");
        verify(refreshTokenService, never()).create(any(User.class));
    }

}
