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
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

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
        user.setEmail("test@mail.com");
        user.setPasswordHash("hashed");
        user.setRole(Role.BUYER);
        user.setBanned(false);
    }

    @Test
    void login_success() {

        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("1234");

        when(loginAttemptService.tryConsume(any())).thenReturn(true);
        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("1234", "hashed"))
                .thenReturn(true);
        when(jwtUtil.generateToken(any(), any()))
                .thenReturn("token");
        when(jwtUtil.getExpirationMs())
                .thenReturn(900000L);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh");

        when(refreshTokenService.create(user)).thenReturn(refreshToken);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("token", response.getToken());
    }

    @Test
    void login_rateLimitExceeded() {

        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");

        when(loginAttemptService.tryConsume(any())).thenReturn(false);

        assertThrows(ArtelierException.class,
                () -> authService.login(request));
    }

    @Test
    void login_userNotFound() {

        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");

        when(loginAttemptService.tryConsume(any())).thenReturn(true);
        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> authService.login(request));
    }

    @Test
    void login_invalidPassword() {

        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("wrong");

        when(loginAttemptService.tryConsume(any())).thenReturn(true);
        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any()))
                .thenReturn(false);

        assertThrows(ArtelierException.class,
                () -> authService.login(request));
    }

    @Test
    void login_userBanned() {

        user.setBanned(true);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");

        when(loginAttemptService.tryConsume(any())).thenReturn(true);
        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.of(user));

        assertThrows(ArtelierException.class,
                () -> authService.login(request));
    }

    @Test
    void register_success() {

        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@mail.com");
        request.setPassword("1234");

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);
        when(passwordEncoder.encode(any()))
                .thenReturn("hashed");
        when(jwtUtil.generateToken(any(), any()))
                .thenReturn("token");
        when(jwtUtil.getExpirationMs())
                .thenReturn(900000L);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh");

        when(refreshTokenService.create(any()))
                .thenReturn(refreshToken);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_emailExists() {

        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@mail.com");

        when(userRepository.existsByEmail(any()))
                .thenReturn(true);

        assertThrows(ArtelierException.class,
                () -> authService.register(request));
    }

    @Test
    void refresh_success() {

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken("old");

        when(refreshTokenService.validate("old")).thenReturn(rt);
        when(jwtUtil.generateToken(any(), any())).thenReturn("newToken");
        when(jwtUtil.getExpirationMs()).thenReturn(900000L);

        RefreshToken newRt = new RefreshToken();
        newRt.setToken("newRefresh");

        when(refreshTokenService.create(user)).thenReturn(newRt);

        AuthResponse response = authService.refresh("old");

        assertNotNull(response);
        assertEquals("newToken", response.getToken());

        verify(refreshTokenService).revokeByUser(user);
    }

    @Test
    void refresh_userBanned() {

        user.setBanned(true);

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);

        when(refreshTokenService.validate(any())).thenReturn(rt);

        assertThrows(ArtelierException.class,
                () -> authService.refresh("token"));
    }


}