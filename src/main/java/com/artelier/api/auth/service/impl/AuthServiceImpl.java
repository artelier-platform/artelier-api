package com.artelier.api.auth.service.impl;

import com.artelier.api.auth.dto.LoginRequest;
import com.artelier.api.auth.dto.RegisterRequest;
import com.artelier.api.auth.dto.AuthResponse;
import com.artelier.api.user.mapper.UserMapper;
import com.artelier.api.user.repository.UserRepository;
import com.artelier.api.auth.security.UserJwtService;
import com.artelier.api.auth.service.AuthService;
import com.artelier.api.user.service.UserEmailService;
import com.artelier.api.auth.service.UserSessionService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserEmailService userEmailService;
    private final UserSessionService userSessionService;
    private final UserJwtService UserJwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptServiceImpl loginAttemptService;
    private final UserMapper userMapper;


    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public AuthResponse register(RegisterRequest registerRequest) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void logout(String refreshToken) {
        throw new UnsupportedOperationException("TODO");
    }
}
