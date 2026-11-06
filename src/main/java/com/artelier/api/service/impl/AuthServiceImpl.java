package com.artelier.api.service.impl;

import com.artelier.api.dto.request.LoginRequest;
import com.artelier.api.dto.request.RegisterRequest;
import com.artelier.api.dto.response.AuthResponse;
import com.artelier.api.mapper.UserMapper;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.AuthService;
import com.artelier.api.service.UserEmailService;
import com.artelier.api.service.UserSessionService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserEmailService userEmailService;
    private final UserSessionService userSessionService;
    private final JwtUtil jwtUtil;
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
