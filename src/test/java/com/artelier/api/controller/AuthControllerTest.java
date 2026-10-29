package com.artelier.api.controller;

import com.artelier.api.config.SecurityConfig;
import com.artelier.api.dto.request.LoginRequest;
import com.artelier.api.dto.request.RegisterRequest;
import com.artelier.api.dto.response.AuthResponse;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // The controller only injects AuthService.
    @MockitoBean private AuthService authService;

    // Security dependencies (JwtFilter / SecurityConfig).
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserRepository userRepository;

    // =========================================================
    // Fixtures
    // =========================================================

    private static final String LOGIN_URL = "/auth/login";

    private static String loginJson(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }

    private static AuthResponse authResponse() {
        return new AuthResponse("access-token", "BUYER", "refresh-token", 3600);
    }

    // =========================================================
    // LOGIN
    // =========================================================

    // Each Arguments: test name, request body, field expected in the validation errors.
    static Stream<Arguments> invalidLoginBodies() {
        return Stream.of(
                Arguments.of("email missing", """
                        { "password": "password123" }
                        """, "email"),
                Arguments.of("email blank", loginJson("", "password123"), "email"),
                Arguments.of("email malformed", loginJson("not-an-email", "password123"), "email"),
                Arguments.of("password missing", """
                        { "email": "test@mail.com" }
                        """, "password"),
                Arguments.of("password blank", loginJson("test@mail.com", ""), "password"),
                Arguments.of("password shorter than 8", loginJson("test@mail.com", "short"), "password")
        );
    }

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("200 – valid credentials return the token pair, role and lifetime")
        void login_success() throws Exception {
            when(authService.login(new LoginRequest("test@mail.com", "password123")))
                    .thenReturn(authResponse());

            // No Authorization header and no CSRF token: the endpoint must be public.
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("test@mail.com", "password123")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Login successful"))
                    .andExpect(jsonPath("$.data.token").value("access-token"))
                    .andExpect(jsonPath("$.data.role").value("BUYER"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.data.expiresIn").value(3600));

            verify(authService).login(new LoginRequest("test@mail.com", "password123"));
        }

        @ParameterizedTest(name = "400 – {0}")
        @MethodSource("com.artelier.api.controller.AuthControllerTest#invalidLoginBodies")
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
                            .content("{ \"email\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid request body. Please check JSON format."));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 – too many failed attempts (rate limit)")
        void login_rateLimited() throws Exception {
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(ArtelierException.badRequest("Too many failed attempts. Try again in 5 minutes"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("test@mail.com", "password123")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Too many failed attempts. Try again in 5 minutes"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("401 – wrong password")
        void login_invalidCredentials() throws Exception {
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(ArtelierException.unauthorized("Invalid credentials"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("test@mail.com", "wrong-password")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid credentials"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("403 – banned user")
        void login_bannedUser() throws Exception {
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(ArtelierException.forbidden("User is banned"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("banned@mail.com", "password123")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("User is banned"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    // =========================================================
    // REGISTER
    // =========================================================

    private static final String REGISTER_URL = "/auth/register";
    private static final String VALID_PHONE = "+573001234567";

    private static String registerJson(String email, String password, String fullName) {
        return """
                {
                  "email": "%s",
                  "password": "%s",
                  "fullName": "%s",
                  "phone": "%s"
                }
                """.formatted(email, password, fullName, VALID_PHONE);
    }

    // Each Argument: test name, request body, field expected in the validation errors.
    static Stream<Arguments> invalidRegisterBodies() {
        return Stream.of(
                Arguments.of("email blank",
                        registerJson("", "password123", "John Doe"), "email"),
                Arguments.of("email malformed",
                        registerJson("not-an-email", "password123", "John Doe"), "email"),
                Arguments.of("password shorter than 8",
                        registerJson("new@mail.com", "short", "John Doe"), "password"),
                Arguments.of("password longer than 72",
                        registerJson("new@mail.com", "a".repeat(73), "John Doe"), "password"),
                Arguments.of("fullName blank",
                        registerJson("new@mail.com", "password123", ""), "fullName"),
                Arguments.of("fullName missing", """
                        { "email": "new@mail.com", "password": "password123" }
                        """, "fullName")
        );
    }

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("201 – valid data creates the account and returns the token pair")
        void register_success() throws Exception {
            RegisterRequest expected =
                    new RegisterRequest("new@mail.com", "password123", "John Doe", VALID_PHONE);
            when(authService.register(expected)).thenReturn(authResponse());

            // No Authorization header and no CSRF token: the endpoint must be public.
            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson("new@mail.com", "password123", "John Doe")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User registered successfully"))
                    .andExpect(jsonPath("$.data.token").value("access-token"))
                    .andExpect(jsonPath("$.data.role").value("BUYER"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.data.expiresIn").value(3600));

            verify(authService).register(expected);
        }

        @Test
        @DisplayName("201 – phone is optional")
        void register_withoutPhone() throws Exception {
            RegisterRequest expected =
                    new RegisterRequest("new@mail.com", "password123", "John Doe", null);
            when(authService.register(expected)).thenReturn(authResponse());

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "new@mail.com",
                                      "password": "password123",
                                      "fullName": "John Doe"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));

            verify(authService).register(expected);
        }

        @Test
        @DisplayName("201 – password of exactly 72 characters is accepted")
        void register_passwordAtMaxLength() throws Exception {
            when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse());

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson("new@mail.com", "a".repeat(72), "John Doe")))
                    .andExpect(status().isCreated());
        }

        @ParameterizedTest(name = "400 – {0}")
        @MethodSource("com.artelier.api.controller.AuthControllerTest#invalidRegisterBodies")
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
                            .content("{ \"email\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid request body. Please check JSON format."));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("409 – email already in use")
        void register_emailConflict() throws Exception {
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(ArtelierException.conflict("Email already in use"));

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson("taken@mail.com", "password123", "John Doe")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Email already in use"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }
}
