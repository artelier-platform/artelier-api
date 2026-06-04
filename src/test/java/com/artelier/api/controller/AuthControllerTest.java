package com.artelier.api.controller;

import com.artelier.api.config.JacksonTestConfig;
import com.artelier.api.dto.request.LoginRequest;
import com.artelier.api.dto.request.RegisterRequest;
import com.artelier.api.dto.request.RefreshRequest;
import com.artelier.api.dto.response.AuthResponse;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;


import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonTestConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("password123");

        AuthResponse response = new AuthResponse(
                "access-token",
                "BUYER",
                "refresh-token",
                3600
        );

        when(authService.login(request)).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("access-token"))
                .andExpect(jsonPath("$.data.role").value("BUYER"));
    }

    // 🔹 REGISTER
    @Test
    void shouldRegisterSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@mail.com");
        request.setPassword("password123");
        request.setFullName("John Doe");
        request.setPhone("+573001234567");

        AuthResponse response = new AuthResponse(
                "access-token",
                "BUYER",
                "refresh-token",
                3600
        );

        when(authService.register(request)).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("access-token"));
    }

    @Test
    void shouldRefreshToken() throws Exception {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("refresh-token");

        AuthResponse response = new AuthResponse(
                "new-access-token",
                "BUYER",
                "new-refresh-token",
                3600
        );

        when(authService.refresh("refresh-token")).thenReturn(response);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("new-access-token"));
    }

    // 🔹 LOGOUT
    @Test
    void shouldLogoutSuccessfully() throws Exception {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("refresh-token");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}