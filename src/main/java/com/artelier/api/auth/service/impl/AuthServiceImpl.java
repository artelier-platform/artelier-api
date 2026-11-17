package com.artelier.api.auth.service.impl;

import com.artelier.api.auth.dto.request.IssuedSession;
import com.artelier.api.auth.dto.request.LoginRequest;
import com.artelier.api.auth.dto.request.RegisterRequest;
import com.artelier.api.auth.dto.response.AuthResponse;
import com.artelier.api.shared.exception.ArtelierException;
import com.artelier.api.user.dto.UserProfileResponse;
import com.artelier.api.user.entity.User;
import com.artelier.api.user.enums.Role;
import com.artelier.api.user.enums.UserStatus;
import com.artelier.api.user.mapper.UserMapper;
import com.artelier.api.user.repository.UserRepository;
import com.artelier.api.auth.security.UserJwtService;
import com.artelier.api.auth.service.AuthService;
import com.artelier.api.user.service.UserEmailService;
import com.artelier.api.auth.service.UserSessionService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

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
    @Transactional
    public UserProfileResponse register(RegisterRequest registerRequest) {

        if (registerRequest == null) {
            throw ArtelierException.badRequest("Registro requerido.");
        }

        userRepository.findByUsernameIgnoreCase(registerRequest.username())
                .ifPresent(existingUser -> {
                    throw ArtelierException.conflict(
                            "El nombre de usuario ya está en uso."
                    );
                });

        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .username(registerRequest.username())
                .passwordHash(passwordEncoder.encode(registerRequest.password()))
                .firstName(registerRequest.firstName())
                .lastName(registerRequest.lastName())
                .phoneCountryCode(registerRequest.phoneCountryCode())
                .phoneNumber(registerRequest.phoneNumber())
                .createdAt(Instant.now())
                .role(Role.BUYER)
                .status(UserStatus.PENDING_VERIFICATION)
                .build();

        userRepository.save(user);

        userEmailService.addEmail(
                userId,
                registerRequest.email()
        );

        return userMapper.toProfileResponse(user);
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
