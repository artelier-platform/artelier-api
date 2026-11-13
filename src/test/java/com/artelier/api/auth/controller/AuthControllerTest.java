package com.artelier.api.auth.controller;

import com.artelier.api.config.WithSecurity;
import com.artelier.api.auth.dto.LoginRequest;
import com.artelier.api.auth.dto.RegisterRequest;
import com.artelier.api.auth.dto.AuthResponse;
import com.artelier.api.user.dto.UserProfileResponse;
import com.artelier.api.user.enums.Role;
import com.artelier.api.user.enums.UserStatus;
import com.artelier.api.shared.exception.ArtelierException;
import com.artelier.api.auth.security.CustomAuthenticationEntryPoint;
import com.artelier.api.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.artelier.api.user.entity.User;

@WebMvcTest(AuthController.class)
@WithSecurity
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuthService authService;
    @MockitoBean private CustomAuthenticationEntryPoint authenticationEntryPoint;

    // =========================================================
    // Fixtures
    // =========================================================

    private static final String LOGIN_URL = "/auth/login";
    private static final String REGISTER_URL = "/auth/register";
    private static final String REFRESH_URL = "/auth/refresh";
    private static final String LOGOUT_URL = "/auth/logout";

    /** Instants are null on purpose: these tests are about HTTP, not date serialization. */
    private static AuthResponse authResponse() {
        UserProfileResponse profile = new UserProfileResponse(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "Geronimo01", "Geronimo", "Rodriguez", "+57", "3001234567",
                null, null, UserStatus.PENDING_VERIFICATION, Role.BUYER, List.of(), null);
        return new AuthResponse("access-token", "refresh-token", "Bearer", 900, profile);
    }

    private static String loginJson(String identifier, String password) {
        return """
                { "identifier": "%s", "password": "%s" }
                """.formatted(identifier, password);
    }

    private static String registerJson(String username, String password, String firstName,
                                       String lastName, String email,
                                       String countryCode, String phone) {
        return """
                {
                  "username": "%s",
                  "password": "%s",
                  "firstName": "%s",
                  "lastName": "%s",
                  "email": "%s",
                  "phoneCountryCode": "%s",
                  "phoneNumber": "%s"
                }
                """.formatted(username, password, firstName, lastName, email, countryCode, phone);
    }

    private static String validRegisterJson() {
        return registerJson("Geronimo01", "password123", "Geronimo", "Rodriguez",
                "user@example.com", "+57", "3001234567");
    }

    private static String refreshJson(String refreshToken) {
        return """
                { "refreshToken": "%s" }
                """.formatted(refreshToken);
    }

    // Each Argument: test name, request body, field expected in the validation errors.
    static Stream<Arguments> invalidLoginBodies() {
        return Stream.of(
                Arguments.of("identifier missing", """
                        { "password": "password123" }
                        """, "identifier"),
                Arguments.of("identifier blank", loginJson("", "password123"), "identifier"),
                Arguments.of("identifier longer than 254",
                        loginJson("a".repeat(255), "password123"), "identifier"),
                Arguments.of("password missing", """
                        { "identifier": "Geronimo01" }
                        """, "password"),
                Arguments.of("password blank", loginJson("Geronimo01", ""), "password"),
                Arguments.of("password shorter than 8", loginJson("Geronimo01", "short"), "password")
        );
    }

    static Stream<Arguments> invalidRegisterBodies() {
        return Stream.of(
                Arguments.of("username with '@'",
                        registerJson("bob@x.com", "password123", "G", "R", "u@example.com", "+57", "3001234567"),
                        "username"),
                Arguments.of("username with a space",
                        registerJson("has space", "password123", "G", "R", "u@example.com", "+57", "3001234567"),
                        "username"),
                Arguments.of("username longer than 12",
                        registerJson("1234567890123", "password123", "G", "R", "u@example.com", "+57", "3001234567"),
                        "username"),
                Arguments.of("username blank",
                        registerJson("", "password123", "G", "R", "u@example.com", "+57", "3001234567"),
                        "username"),
                Arguments.of("password shorter than 8",
                        registerJson("Geronimo01", "short", "G", "R", "u@example.com", "+57", "3001234567"),
                        "password"),
                Arguments.of("password longer than 72",
                        registerJson("Geronimo01", "a".repeat(73), "G", "R", "u@example.com", "+57", "3001234567"),
                        "password"),
                Arguments.of("email malformed",
                        registerJson("Geronimo01", "password123", "G", "R", "not-an-email", "+57", "3001234567"),
                        "email"),
                Arguments.of("firstName blank",
                        registerJson("Geronimo01", "password123", "", "R", "u@example.com", "+57", "3001234567"),
                        "firstName"),
                Arguments.of("lastName blank",
                        registerJson("Geronimo01", "password123", "G", "", "u@example.com", "+57", "3001234567"),
                        "lastName"),
                Arguments.of("country code without '+'",
                        registerJson("Geronimo01", "password123", "G", "R", "u@example.com", "57", "3001234567"),
                        "phoneCountryCode"),
                Arguments.of("phone number with letters",
                        registerJson("Geronimo01", "password123", "G", "R", "u@example.com", "+57", "abc"),
                        "phoneNumber"),
                Arguments.of("phone number longer than 15",
                        registerJson("Geronimo01", "password123", "G", "R", "u@example.com", "+57", "1234567890123456"),
                        "phoneNumber"),
                Arguments.of("empty object", "{}", "username")
        );
    }

    // =========================================================
    // LOGIN
    // =========================================================

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("200 – returns tokens and the user profile, without sensitive data")
        void login_success() throws Exception {
            when(authService.login(new LoginRequest("Geronimo01", "password123")))
                    .thenReturn(authResponse());

            // No Authorization header and no CSRF token: the endpoint must be public.
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("Geronimo01", "password123")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Login successful"))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.expiresIn").value(900))
                    .andExpect(jsonPath("$.data.user.username").value("Geronimo01"))
                    .andExpect(jsonPath("$.data.user.role").value("BUYER"))
                    .andExpect(jsonPath("$.data.user.status").value("PENDING_VERIFICATION"))
                    .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());

            verify(authService).login(new LoginRequest("Geronimo01", "password123"));
        }

        @ParameterizedTest(name = "400 – {0}")
        @MethodSource("com.artelier.api.auth.controller.AuthControllerTest#invalidLoginBodies")
        @DisplayName("400 – invalid body is rejected before reaching the service")
        void login_invalidBody(String name, String body, String field) throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.data[?(@.field == '" + field + "')]").isNotEmpty());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 – malformed JSON")
        void login_malformedJson() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"identifier\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid request body. Please check JSON format."));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 – too many failed attempts")
        void login_rateLimited() throws Exception {
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(ArtelierException.badRequest("Too many failed attempts. Try again in 5 minutes"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("Geronimo01", "password123")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Too many failed attempts. Try again in 5 minutes"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("401 – invalid credentials")
        void login_invalidCredentials() throws Exception {
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(ArtelierException.unauthorized("Invalid credentials"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("Geronimo01", "wrong-password")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid credentials"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("403 – suspended account")
        void login_suspendedAccount() throws Exception {
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(ArtelierException.forbidden("Account is suspended"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("Geronimo01", "password123")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Account is suspended"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    // =========================================================
    // REGISTER
    // =========================================================

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("201 – creates the account and returns the tokens")
        void register_success() throws Exception {
            RegisterRequest expected = new RegisterRequest(
                    "Geronimo01", "password123", "Geronimo", "Rodriguez",
                    "user@example.com", "+57", "3001234567");
            when(authService.register(expected)).thenReturn(authResponse());

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRegisterJson()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User registered successfully"))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.data.user.username").value("Geronimo01"));

            verify(authService).register(expected);
        }

        @Test
        @DisplayName("201 – password of exactly 72 characters is accepted")
        void register_passwordAtMaxLength() throws Exception {
            when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse());

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson("Geronimo01", "a".repeat(72), "Geronimo",
                                    "Rodriguez", "user@example.com", "+57", "3001234567")))
                    .andExpect(status().isCreated());
        }

        @ParameterizedTest(name = "400 – {0}")
        @MethodSource("com.artelier.api.auth.controller.AuthControllerTest#invalidRegisterBodies")
        @DisplayName("400 – invalid body is rejected before reaching the service")
        void register_invalidBody(String name, String body, String field) throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.data[?(@.field == '" + field + "')]").isNotEmpty());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 – malformed JSON")
        void register_malformedJson() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"username\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid request body. Please check JSON format."));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("409 – username or email already in use")
        void register_conflict() throws Exception {
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(ArtelierException.conflict("Username already in use"));

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRegisterJson()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Username already in use"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    // =========================================================
    // REFRESH
    // =========================================================

    @Nested
    @DisplayName("POST /auth/refresh")
    class Refresh {

        @Test
        @DisplayName("200 – returns the rotated token pair")
        void refresh_success() throws Exception {
            when(authService.refresh("refresh-token")).thenReturn(authResponse());

            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshJson("refresh-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Token refreshed"))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"));

            verify(authService).refresh("refresh-token");
        }

        @Test
        @DisplayName("400 – missing refresh token")
        void refresh_missingToken() throws Exception {
            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 – blank refresh token")
        void refresh_blankToken() throws Exception {
            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshJson("")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("401 – invalid, expired, revoked or reused refresh token")
        void refresh_invalidToken() throws Exception {
            when(authService.refresh("invalid-token"))
                    .thenThrow(ArtelierException.unauthorized("Invalid refresh token"));

            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshJson("invalid-token")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid refresh token"));
        }

        @Test
        @DisplayName("403 – suspended account cannot refresh")
        void refresh_suspendedAccount() throws Exception {
            when(authService.refresh("refresh-token"))
                    .thenThrow(ArtelierException.forbidden("Account is suspended"));

            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshJson("refresh-token")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Account is suspended"));
        }
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    @Nested
    @DisplayName("POST /auth/logout")
    class Logout {

        @Test
        @DisplayName("200 – logs out the session of that refresh token")
        void logout_success() throws Exception {
            mockMvc.perform(post(LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshJson("refresh-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));

            verify(authService).logout("refresh-token");
        }

        @Test
        @DisplayName("400 – missing refresh token")
        void logout_missingToken() throws Exception {
            mockMvc.perform(post(LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 – blank refresh token")
        void logout_blankToken() throws Exception {
            mockMvc.perform(post(LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshJson("")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("401 – unknown refresh token")
        void logout_invalidToken() throws Exception {
            doThrow(ArtelierException.unauthorized("Invalid refresh token"))
                    .when(authService).logout("invalid-token");

            mockMvc.perform(post(LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshJson("invalid-token")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid refresh token"));
        }
    }
}