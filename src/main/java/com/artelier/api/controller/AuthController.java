package com.artelier.api.controller;

import com.artelier.api.dto.request.LoginRequest;
import com.artelier.api.dto.request.RefreshRequest;
import com.artelier.api.dto.request.RegisterRequest;
import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.AuthResponse;
import com.artelier.api.dto.response.swagger.SwaggerResponses;
import com.artelier.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing authentication and session management endpoints.
 *
 * <p>All endpoints are mounted under {@code /auth} and are <b>public</b>: they
 * do not require a Bearer token. They are explicitly excluded from the global
 * {@code bearerAuth} security requirement through {@link SecurityRequirements}
 * so Swagger UI does not send an {@code Authorization} header for them.</p>
 *
 * <h2>Session model</h2>
 * <ul>
 *   <li><b>Access token</b>: short-lived JWT sent in the {@code Authorization} header.</li>
 *   <li><b>Refresh token</b>: long-lived, single-use credential. Every refresh
 *       rotates the pair and revokes the previous refresh token.</li>
 *   <li><b>Logout</b>: revokes all refresh tokens of the user. Access tokens
 *       already issued remain valid until they expire.</li>
 * </ul>
 *
 * <h2>Response conventions</h2>
 * <ul>
 *   <li>Every response is wrapped in {@link AppResponse}
 *       ({@code success}, {@code message}, optional {@code data}).</li>
 *   <li>Errors are translated to {@link AppResponse} with {@code success = false}
 *       by the global exception handler.</li>
 * </ul>
 *
 * <p>This class only handles HTTP concerns (routing, validation, status codes
 * and documentation). Credential checks, token generation, rate limiting and
 * ban verification live in {@link AuthService}.</p>
 */
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
        They expire automatically after the configured lifetime
        (see `expiresIn`, expressed in seconds).

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

    // =========================================================
    // OpenAPI shared descriptions
    // =========================================================

    private static final String DESC_VALIDATION_FAILED =
            "Invalid request body: a required field is missing or has an invalid format";
    private static final String DESC_INVALID_REFRESH_TOKEN =
            "Invalid, expired, revoked or already used refresh token";
    private static final String DESC_BANNED = "User account is banned";
    private static final String DESC_SERVER_ERROR = "Unexpected server error";

    // =========================================================
    // Dependencies
    // =========================================================

    private final AuthService authService;

    // =========================================================
    // LOGIN
    // =========================================================

    /**
     * Authenticates a user with email and password.
     *
     * @param request login credentials
     * @return {@code 200} with the access token, refresh token, role and
     *         access token lifetime
     */
    @SecurityRequirements
    @Operation(
            operationId = "login",
            summary = "Authenticate user",
            description = """
        Authenticates a registered user using email and password credentials.

        ## Successful Authentication

        When authentication succeeds, the API returns:

        - JWT access token
        - Refresh token
        - User role
        - Access token lifetime in seconds (`expiresIn`)

        ## Security

        Passwords are validated against securely stored password hashes.

        ## Rate Limiting

        Failed login attempts are rate limited.

        After too many failed attempts, authentication is temporarily
        blocked for approximately 5 minutes.

        ## Account Restrictions

        Authentication is denied if the user account has been banned.
        The ban is only reported after the password is verified, so the API
        does not reveal whether an email is registered.
        """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.AuthResponseBody.class),
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
            description = "Invalid request body or too many failed login attempts",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "validation-failed",
                                    summary = "Missing or malformed fields",
                                    value = """
                                        {
                                          "success": false,
                                          "message": "Validation failed",
                                          "data": [
                                            {
                                              "field": "email",
                                              "message": "must be a well-formed email address"
                                            }
                                          ]
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "rate-limit",
                                    summary = "Login temporarily blocked",
                                    value = """
                                        {
                                          "success": false,
                                          "message": "Too many failed attempts. Try again in 5 minutes"
                                        }
                                        """
                            )
                    }
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials: unknown email or wrong password",
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
            description = DESC_BANNED,
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
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    @PostMapping("/login")
    public ResponseEntity<AppResponse<AuthResponse>> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Login credentials",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(
                                    name = "login-request",
                                    value = """
                                        {
                                          "email": "user@example.com",
                                          "password": "MySecurePassword123"
                                        }
                                        """
                            )
                    )
            )
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse auth = authService.login(request);
        return ResponseEntity.ok(AppResponse.success("Login successful", auth));
    }

    // =========================================================
    // REGISTER
    // =========================================================

    /**
     * Registers a new {@code BUYER} account and starts a session.
     *
     * @param request registration data
     * @return {@code 201} with the access token, refresh token, role and
     *         access token lifetime
     */
    @SecurityRequirements
    @Operation(
            operationId = "register",
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
                    schema = @Schema(implementation = SwaggerResponses.AuthResponseBody.class),
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
            description = "Invalid registration data",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "validation-failed",
                            value = """
                                {
                                  "success": false,
                                  "message": "Validation failed",
                                  "data": [
                                    {
                                      "field": "password",
                                      "message": "size must be between 8 and 72"
                                    }
                                  ]
                                }
                                """
                    )
            )
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
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    @PostMapping("/register")
    public ResponseEntity<AppResponse<AuthResponse>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Data of the account to create",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RegisterRequest.class)
                    )
            )
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse auth = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AppResponse.success("User registered successfully", auth));
    }

    // =========================================================
    // REFRESH
    // =========================================================

    /**
     * Exchanges a valid refresh token for a new token pair (rotation).
     *
     * @param request body holding the current refresh token
     * @return {@code 200} with the new access and refresh tokens
     */
    @SecurityRequirements
    @Operation(
            operationId = "refreshToken",
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
            description = "Token refreshed successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.AuthResponseBody.class),
                    examples = @ExampleObject(
                            name = "refresh-success",
                            value = """
                                {
                                  "success": true,
                                  "message": "Token refreshed",
                                  "data": {
                                    "token": "eyJhbGciOiJIUzI1NiJ9.new-access-token",
                                    "role": "BUYER",
                                    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.new-refresh-token",
                                    "expiresIn": 3600
                                  }
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = DESC_VALIDATION_FAILED,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = DESC_INVALID_REFRESH_TOKEN,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "invalid-refresh-token",
                            value = """
                                {
                                  "success": false,
                                  "message": "Invalid refresh token"
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = DESC_BANNED,
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
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    @PostMapping("/refresh")
    public ResponseEntity<AppResponse<AuthResponse>> refresh(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Refresh token issued at login, registration or the previous refresh",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RefreshRequest.class),
                            examples = @ExampleObject(
                                    name = "refresh-request",
                                    value = """
                                        {
                                          "refreshToken": "eyJhbGciOiJIUzI1NiJ9.refresh-token"
                                        }
                                        """
                            )
                    )
            )
            @Valid @RequestBody RefreshRequest request
    ) {
        AuthResponse auth = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(AppResponse.success("Token refreshed", auth));
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    /**
     * Ends the session by revoking every refresh token of the user.
     *
     * @param request body holding a valid refresh token of the session
     * @return {@code 200} with a confirmation message
     */
    @SecurityRequirements
    @Operation(
            operationId = "logout",
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
        Clients should discard them locally on logout.
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
            responseCode = "400",
            description = DESC_VALIDATION_FAILED,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = DESC_INVALID_REFRESH_TOKEN,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "invalid-refresh-token",
                            value = """
                                {
                                  "success": false,
                                  "message": "Invalid refresh token"
                                }
                                """
                    )
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    @PostMapping("/logout")
    public ResponseEntity<AppResponse<Void>> logout(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Refresh token of the session to terminate",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RefreshRequest.class),
                            examples = @ExampleObject(
                                    name = "logout-request",
                                    value = """
                                        {
                                          "refreshToken": "eyJhbGciOiJIUzI1NiJ9.refresh-token"
                                        }
                                        """
                            )
                    )
            )
            @Valid @RequestBody RefreshRequest request
    ) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(AppResponse.success("Logged out successfully"));
    }
}