package com.artelier.api.service;

import com.artelier.api.dto.request.LoginRequest;
import com.artelier.api.dto.request.RegisterRequest;
import com.artelier.api.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest loginRequest);
    AuthResponse  register (RegisterRequest registerRequest);
    AuthResponse refresh(String refreshToken);
    void logout(String refreshToken);

}
