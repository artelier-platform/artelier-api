package com.artelier.api.auth.service;

import com.artelier.api.auth.dto.LoginRequest;
import com.artelier.api.auth.dto.RegisterRequest;
import com.artelier.api.auth.dto.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest loginRequest);
    AuthResponse  register (RegisterRequest registerRequest);
    AuthResponse refresh(String refreshToken);
    void logout(String refreshToken);

}
