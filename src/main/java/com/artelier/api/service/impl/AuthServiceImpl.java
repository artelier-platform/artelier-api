package com.artelier.api.service.impl;

import com.artelier.api.dto.request.LoginRequest;
import com.artelier.api.dto.request.RegisterRequest;
import com.artelier.api.dto.response.AuthResponse;
import com.artelier.api.entity.RefreshToken;
import com.artelier.api.entity.User;
import com.artelier.api.entity.enums.Role;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.AuthService;
import com.artelier.api.service.RefreshTokenService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptServiceImpl loginAttemptService;

    @Override
    public AuthResponse login(LoginRequest loginRequest) {

        if (!loginAttemptService.tryConsume(loginRequest.getEmail())) {
            throw ArtelierException.badRequest(
                    "Too many failed attempts. Try again in 5 minutes"
            );
        }

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> ArtelierException.notFound("User not found"));

        if (user.isBanned()) {
            throw ArtelierException.forbidden("User is banned");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw ArtelierException.unauthorized("Invalid credentials");
        }

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw ArtelierException.conflict("Email already in use");
        }

        User user = new User();
        user.setRole(Role.BUYER);
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFullName(registerRequest.getFullName());
        user.setPhone(registerRequest.getPhone());

        userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Override
    public void logout(String refreshToken) {
        RefreshToken rt = refreshTokenService.validate(refreshToken);
        refreshTokenService.revokeByUser(rt.getUser());
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        RefreshToken rt = refreshTokenService.validate(refreshToken);
        User user = rt.getUser();
        if (user.isBanned()) {
            throw ArtelierException.forbidden("User is banned");
        }
        refreshTokenService.revokeByUser(user);
        String newAccessToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name()
        );
        RefreshToken newRefreshToken = refreshTokenService.create(user);

        return new AuthResponse(
                newAccessToken,
                user.getRole().name(),
                newRefreshToken.getToken(),
                jwtUtil.getExpirationMs() / 1000
        );
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = refreshTokenService.create(user).getToken();
        long expiresIn = jwtUtil.getExpirationMs() / 1000;
        return new AuthResponse(
                token,
                user.getRole().name(),
                refreshToken,
                expiresIn
        );
    }


}
