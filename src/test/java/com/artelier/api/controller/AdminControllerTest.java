package com.artelier.api.controller;

import com.artelier.api.config.SecurityConfig;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminController")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    @Nested
    @DisplayName("PATCH /admin/users/{id}/ban")
    class BanUser {

        @Test
        @DisplayName("200 – admin bans a user successfully")
        @WithMockUser(roles = "ADMIN")
        void banUser_success() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(adminService).setBanned(id, true);

            mockMvc.perform(patch("/admin/users/{id}/ban", id).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User banned"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(adminService).setBanned(id, true);
        }

        @Test
        @DisplayName("403 – non-admin user is rejected")
        @WithMockUser(roles = "BUYER")
        void banUser_forbidden() throws Exception {
            mockMvc.perform(patch("/admin/users/{id}/ban", UUID.randomUUID()).with(csrf()))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(adminService);
        }

        @Test
        @DisplayName("401 – unauthenticated request is rejected")
        void banUser_unauthorized() throws Exception {
            mockMvc.perform(patch("/admin/users/{id}/ban", UUID.randomUUID()).with(csrf()))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(adminService);
        }
    }

    @Nested
    @DisplayName("PATCH /admin/users/{id}/unban")
    class UnbanUser {

        @Test
        @DisplayName("200 – admin unbans a user successfully")
        @WithMockUser(roles = "ADMIN")
        void unbanUser_success() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(adminService).setBanned(id, false);

            mockMvc.perform(patch("/admin/users/{id}/unban", id).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User unbanned"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(adminService).setBanned(id, false);
        }

        @Test
        @DisplayName("403 – non-admin user is rejected")
        @WithMockUser(roles = "BUYER")
        void unbanUser_forbidden() throws Exception {
            mockMvc.perform(patch("/admin/users/{id}/unban", UUID.randomUUID()).with(csrf()))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(adminService);
        }

        @Test
        @DisplayName("401 – unauthenticated request is rejected")
        void unbanUser_unauthorized() throws Exception {
            mockMvc.perform(patch("/admin/users/{id}/unban", UUID.randomUUID()).with(csrf()))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(adminService);
        }
    }
}