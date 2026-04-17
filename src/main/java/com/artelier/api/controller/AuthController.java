package com.artelier.api.controller;

import com.artelier.api.dto.request.LoginRequest;
import com.artelier.api.dto.request.RefreshRequest;
import com.artelier.api.dto.request.RegisterRequest;
import com.artelier.api.dto.response.ApiResponse;
import com.artelier.api.dto.response.AuthResponse;
import com.artelier.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/auth")
@Tag(
        name = "Authentication",
        description = "Endpoints for user authentication and session management"
)
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Login",
            description = "Authenticates a user and returns JWT tokens"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            )
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Login credentials",
                    required = true
            )
            LoginRequest request
    ) {
        AuthResponse auth = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success("Login successful", auth)
        );
    }

    @Operation(
            summary = "Register",
            description = "Registers a new BUYER account"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Email already in use"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data"
            )
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid
            @RequestBody RegisterRequest request
    ) {
        AuthResponse auth = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "User registered successfully",
                                auth
                        )
                );
    }

    @Operation(
            summary = "Refresh token",
            description = "Generates a new JWT using a valid refresh token"
    )
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestBody RefreshRequest request
    ) {

        AuthResponse auth =
                authService.refresh(request.getRefreshToken());

        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed", auth)
        );
    }

    @Operation(
            summary = "Logout",
            description = "Revokes the refresh token"
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody RefreshRequest request
    ) {

        authService.logout(request.getRefreshToken());

        return ResponseEntity.ok(
                ApiResponse.success("Logged out successfully")
        );
    }
}