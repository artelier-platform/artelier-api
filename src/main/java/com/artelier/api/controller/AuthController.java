package com.artelier.api.controller;

import com.artelier.api.dto.request.LoginRequest;
import com.artelier.api.dto.request.RefreshRequest;
import com.artelier.api.dto.request.RegisterRequest;
import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.AuthResponse;
import com.artelier.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/auth")
@Tag(
        name = "Authentication",
        description = """
        Authentication and session management endpoints.
        
        ## Authentication Flow
        
        1. Register a new account using `POST /auth/register`
        2. Authenticate with `POST /auth/login`
        3. Use the returned JWT access token in the `Authorization` header
        
           Authorization: Bearer <token>
        
        4. When the access token expires, obtain a new one using
           `POST /auth/refresh`
        5. When the user signs out, revoke active sessions using
           `POST /auth/logout`
        
        ## Roles
        
        | Role | Description |
        |------|-------------|
        | `BUYER` | Standard customer account |
        | `ADMIN` | Administrative account |
        
        ## Access Tokens
        
        Access tokens are JWTs used to authenticate API requests.
        They expire automatically after the configured lifetime.
        
        ## Refresh Tokens
        
        Refresh tokens are long-lived credentials used to obtain
        new access tokens without requiring the user to log in again.
        
        Every refresh operation invalidates the previous refresh token
        and issues a new token pair.
        
        ## Account Restrictions
        
        Banned users cannot:
        
        - Authenticate
        - Refresh existing sessions
        
        Authentication attempts from banned accounts are rejected.
        """
)
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Authenticate user",
            description = """
        Authenticates a registered user using email and password credentials.
        
        ## Successful Authentication
        
        When authentication succeeds, the API returns:
        
        - JWT access token
        - Refresh token
        - User role
        - Token expiration time
        
        ## Security
        
        Passwords are validated against securely stored password hashes.
        
        ## Rate Limiting
        
        Failed login attempts are rate limited.
        
        After too many failed attempts, authentication is temporarily
        blocked for approximately 5 minutes.
        
        ## Account Restrictions
        
        Authentication is denied if the user account has been banned.
        """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "login-success",
                            value = """
                                {
                                  "success": true,
                                  "message": "Login successful",
                                  "data": {
                                    "token": "eyJhbGciOiJIUzI1NiJ9.access-token",
                                    "role": "BUYER",
                                    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.refresh-token",
                                    "expiresIn": 3600
                                  }
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Too many failed login attempts",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "rate-limit",
                            value = """
                                {
                                  "success": false,
                                  "message": "Too many failed attempts. Try again in 5 minutes"
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "invalid-credentials",
                            value = """
                                {
                                  "success": false,
                                  "message": "Invalid credentials"
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "User account is banned",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "banned-user",
                            value = """
                                {
                                  "success": false,
                                  "message": "User is banned"
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "user-not-found",
                            value = """
                                {
                                  "success": false,
                                  "message": "User not found"
                                }
                                """
                    )
            )
    )
    @PostMapping("/login")
    public ResponseEntity<AppResponse<AuthResponse>> login(
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
                AppResponse.success("Login successful", auth)
        );
    }

    @Operation(
            summary = "Register new account",
            description = """
        Creates a new user account and immediately returns
        authenticated session tokens.
        
        ## Default Role
        
        Newly registered users are assigned the `BUYER` role.
        
        Administrative accounts cannot be created through this endpoint.
        
        ## Automatic Authentication
        
        After registration:
        
        - Access token is generated
        - Refresh token is generated
        - User is considered authenticated
        
        No additional login request is required.
        
        ## Email Uniqueness
        
        Email addresses must be unique across all accounts.
        """
    )
    @ApiResponse(
            responseCode = "201",
            description = "User registered successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "registration-success",
                            value = """
                                {
                                  "success": true,
                                  "message": "User registered successfully",
                                  "data": {
                                    "token": "eyJhbGciOiJIUzI1NiJ9.access-token",
                                    "role": "BUYER",
                                    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.refresh-token",
                                    "expiresIn": 3600
                                  }
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid registration data"
    )
    @ApiResponse(
            responseCode = "409",
            description = "Email already in use",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "email-conflict",
                            value = """
                                {
                                  "success": false,
                                  "message": "Email already in use"
                                }
                                """
                    )
            )
    )
    @PostMapping("/register")
    public ResponseEntity<AppResponse<AuthResponse>> register(
            @Valid
            @RequestBody RegisterRequest request
    ) {
        AuthResponse auth = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        AppResponse.success(
                                "User registered successfully",
                                auth
                        )
                );
    }

    @Operation(
            summary = "Refresh access token",
            description = """
        Generates a new authenticated session using a valid refresh token.
        
        ## Token Rotation
        
        Refresh tokens are single-use.
        
        When a refresh succeeds:
        
        - Previous refresh token is revoked
        - New access token is issued
        - New refresh token is issued
        
        Clients must replace stored tokens with the newly returned values.
        
        ## Security
        
        Reusing an old refresh token is not allowed.
        
        ## Account Restrictions
        
        Refresh requests are rejected for banned users.
        """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Token refreshed successfully"
    )
    @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token"
    )
    @ApiResponse(
            responseCode = "403",
            description = "User account is banned"
    )
    @PostMapping("/refresh")
    public ResponseEntity<AppResponse<AuthResponse>> refresh(
            @RequestBody RefreshRequest request
    ) {

        AuthResponse auth =
                authService.refresh(request.getRefreshToken());

        return ResponseEntity.ok(
                AppResponse.success("Token refreshed", auth)
        );
    }

    @Operation(
            summary = "Logout user",
            description = """
        Terminates the current authenticated session.
        
        ## Session Revocation
        
        The provided refresh token is validated and all active
        refresh tokens belonging to the user are revoked.
        
        After logout:
        
        - Existing refresh tokens become unusable
        - New access tokens cannot be generated
        
        Users must authenticate again to create a new session.
        
        ## Access Tokens
        
        Previously issued access tokens remain valid until expiration.
        """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Logout completed successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "logout-success",
                            value = """
                                {
                                  "success": true,
                                  "message": "Logged out successfully"
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token"
    )
    @PostMapping("/logout")
    public ResponseEntity<AppResponse<Void>> logout(
            @RequestBody RefreshRequest request
    ) {

        authService.logout(request.getRefreshToken());

        return ResponseEntity.ok(
                AppResponse.success("Logged out successfully")
        );
    }
}