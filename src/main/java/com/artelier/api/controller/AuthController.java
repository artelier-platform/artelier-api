package com.artelier.api.controller;

import com.artelier.api.dto.request.LoginRequest;
import com.artelier.api.dto.request.RefreshTokenRequest;
import com.artelier.api.dto.request.RegisterRequest;
import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.AuthResponse;
import com.artelier.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AppResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse auth = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AppResponse.success("User registered successfully", auth));
    }

    @PostMapping("/login")
    public ResponseEntity<AppResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse auth = authService.login(request);
        return ResponseEntity.ok(AppResponse.success("Login successful", auth));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AppResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        AuthResponse auth = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(AppResponse.success("Token refreshed", auth));
    }

    @PostMapping("/logout")
    public ResponseEntity<AppResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(AppResponse.success("Logged out successfully"));
    }
}