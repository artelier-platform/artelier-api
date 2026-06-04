package com.artelier.api.controller;

import com.artelier.api.config.JacksonTestConfig;
import com.artelier.api.config.SecurityConfig;
import com.artelier.api.dto.request.CategoryRequest;
import com.artelier.api.dto.response.CategoryResponse;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JacksonTestConfig.class})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────────
    // GET /categories
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("GET /categories")
    class GetAll {

        @Test
        @DisplayName("200 – returns list of categories")
        void getAll_success() throws Exception {
            List<CategoryResponse> categories = List.of(
                    buildCategoryResponse("Ceramics", "ceramics"),
                    buildCategoryResponse("Textiles", "textiles")
            );
            when(categoryService.getAll()).thenReturn(categories);

            mockMvc.perform(get("/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Categories fetched"))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].name").value("Ceramics"))
                    .andExpect(jsonPath("$.data[1].name").value("Textiles"));

            verify(categoryService).getAll();
        }

        @Test
        @DisplayName("200 – returns empty list when no categories exist")
        void getAll_empty() throws Exception {
            when(categoryService.getAll()).thenReturn(List.of());

            mockMvc.perform(get("/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("200 – endpoint is public, no auth required")
        void getAll_isPublic() throws Exception {
            when(categoryService.getAll()).thenReturn(List.of());

            // Intencionalmente sin @WithMockUser
            mockMvc.perform(get("/categories"))
                    .andExpect(status().isOk());
        }
    }

    // ─────────────────────────────────────────────
    // POST /categories
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("POST /categories")
    class Create {

        @Test
        @DisplayName("201 – admin creates a category successfully")
        @WithMockUser(roles = "ADMIN")
        void create_success() throws Exception {
            CategoryRequest request = buildRequest("Jewelry", "jewelry", "Handmade jewelry");
            CategoryResponse response = buildCategoryResponse("Jewelry", "jewelry");

            when(categoryService.create(any(CategoryRequest.class))).thenReturn(response);

            mockMvc.perform(post("/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Category created"))
                    .andExpect(jsonPath("$.data.name").value("Jewelry"))
                    .andExpect(jsonPath("$.data.slug").value("jewelry"));

            verify(categoryService).create(any(CategoryRequest.class));
        }

        @Test
        @DisplayName("400 – blank name fails validation")
        @WithMockUser(roles = "ADMIN")
        void create_blankName_returns400() throws Exception {
            CategoryRequest invalid = buildRequest("", "jewelry", null);

            mockMvc.perform(post("/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(categoryService);
        }

        @Test
        @DisplayName("400 – blank slug fails validation")
        @WithMockUser(roles = "ADMIN")
        void create_blankSlug_returns400() throws Exception {
            CategoryRequest invalid = buildRequest("Jewelry", "", null);

            mockMvc.perform(post("/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(categoryService);
        }

        @Test
        @DisplayName("400 – missing body returns bad request")
        @WithMockUser(roles = "ADMIN")
        void create_missingBody_returns400() throws Exception {
            mockMvc.perform(post("/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("403 – non-admin user cannot create categories")
        @WithMockUser(roles = "BUYER")
        void create_forbidden() throws Exception {
            mockMvc.perform(post("/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest("Jewelry", "jewelry", null))))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(categoryService);
        }

        @Test
        @DisplayName("401 – unauthenticated request is rejected")
        void create_unauthorized() throws Exception {
            mockMvc.perform(post("/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest("Jewelry", "jewelry", null))))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(categoryService);
        }
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────
    private CategoryRequest buildRequest(String name, String slug, String description) {
        CategoryRequest r = new CategoryRequest();
        r.setName(name);
        r.setSlug(slug);
        r.setDescription(description);
        return r;
    }

    private CategoryResponse buildCategoryResponse(String name, String slug) {
        CategoryResponse r = new CategoryResponse();
        r.setId(UUID.randomUUID());
        r.setName(name);
        r.setSlug(slug);
        return r;
    }
}