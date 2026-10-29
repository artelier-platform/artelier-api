package com.artelier.api.controller;

import com.artelier.api.config.JacksonTestConfig;
import com.artelier.api.config.SecurityConfig;
import com.artelier.api.dto.response.CategoryResponse;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link CategoryController}. They run with the real {@link SecurityConfig}
 * (filters enabled), so every contract test is also an anonymous-access test of the public endpoint.
 */
@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JacksonTestConfig.class})
@DisplayName("CategoryController")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    private static CategoryResponse category(UUID id, String name, String slug, String description) {
        return new CategoryResponse(id, name, slug, description);
    }

    @Nested
    @DisplayName("GET /categories")
    class GetAll {

        @Test
        @DisplayName("200 – anonymous visitor gets every category field in the standard envelope")
        void mapsAllFields() throws Exception {
            UUID ceramicsId = UUID.randomUUID();
            UUID jewelryId = UUID.randomUUID();
            when(categoryService.getAll()).thenReturn(List.of(
                    category(ceramicsId, "Ceramics", "ceramics", "Handmade ceramic products"),
                    category(jewelryId, "Jewelry", "jewelry", "Handcrafted necklaces and rings")));

            mockMvc.perform(get("/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Categories fetched"))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value(ceramicsId.toString()))
                    .andExpect(jsonPath("$.data[0].name").value("Ceramics"))
                    .andExpect(jsonPath("$.data[0].slug").value("ceramics"))
                    .andExpect(jsonPath("$.data[0].description").value("Handmade ceramic products"))
                    .andExpect(jsonPath("$.data[1].id").value(jewelryId.toString()))
                    .andExpect(jsonPath("$.data[1].name").value("Jewelry"))
                    .andExpect(jsonPath("$.data[1].slug").value("jewelry"))
                    .andExpect(jsonPath("$.data[1].description").value("Handcrafted necklaces and rings"));
        }

        @Test
        @DisplayName("200 – keeps the order returned by the service")
        void preservesOrdering() throws Exception {
            when(categoryService.getAll()).thenReturn(List.of(
                    category(UUID.randomUUID(), "Textiles", "textiles", null),
                    category(UUID.randomUUID(), "Ceramics", "ceramics", null),
                    category(UUID.randomUUID(), "Jewelry", "jewelry", null)));

            mockMvc.perform(get("/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].slug").value("textiles"))
                    .andExpect(jsonPath("$.data[1].slug").value("ceramics"))
                    .andExpect(jsonPath("$.data[2].slug").value("jewelry"));
        }

        @Test
        @DisplayName("200 – category without description is still returned")
        void categoryWithoutDescription() throws Exception {
            when(categoryService.getAll()).thenReturn(List.of(
                    category(UUID.randomUUID(), "Ceramics", "ceramics", null)));

            mockMvc.perform(get("/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].slug").value("ceramics"));
        }

        @Test
        @DisplayName("200 – no categories returns an empty list, not an error")
        void emptyList() throws Exception {
            when(categoryService.getAll()).thenReturn(List.of());

            mockMvc.perform(get("/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Categories fetched"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("200 – responds with a JSON body")
        void respondsWithJson() throws Exception {
            when(categoryService.getAll()).thenReturn(List.of());

            mockMvc.perform(get("/categories"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Delegates exactly once to the service and ignores unknown query parameters")
        void delegatesOnce() throws Exception {
            when(categoryService.getAll()).thenReturn(List.of());

            mockMvc.perform(get("/categories").param("unknown", "value"))
                    .andExpect(status().isOk());

            verify(categoryService, times(1)).getAll();
            verifyNoMoreInteractions(categoryService);
        }

        @Test
        @DisplayName("500 – unexpected failure is mapped to the generic error body")
        void unexpectedFailure() throws Exception {
            when(categoryService.getAll()).thenThrow(new IllegalStateException("db down"));

            mockMvc.perform(get("/categories"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Something went wrong"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("ArtelierException keeps its own status and message")
        void artelierExceptionIsTranslated() throws Exception {
            when(categoryService.getAll())
                    .thenThrow(new ArtelierException("Catalog unavailable", HttpStatus.SERVICE_UNAVAILABLE));

            mockMvc.perform(get("/categories"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Catalog unavailable"));
        }
    }

    @Nested
    @DisplayName("Access rules")
    class AccessRules {

        @Test
        @DisplayName("200 – authenticated BUYER can list categories")
        @WithMockUser(roles = "BUYER")
        void buyerAllowed() throws Exception {
            when(categoryService.getAll()).thenReturn(List.of());

            mockMvc.perform(get("/categories"))
                    .andExpect(status().isOk());

            verify(categoryService).getAll();
        }

        @Test
        @DisplayName("200 – authenticated ADMIN can list categories")
        @WithMockUser(roles = "ADMIN")
        void adminAllowed() throws Exception {
            when(categoryService.getAll()).thenReturn(List.of());

            mockMvc.perform(get("/categories"))
                    .andExpect(status().isOk());

            verify(categoryService).getAll();
        }

        @Test
        @DisplayName("401 – anonymous POST is not public and never reaches the service")
        void anonymousPostRejected() throws Exception {
            mockMvc.perform(post("/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Ceramics\",\"slug\":\"ceramics\"}"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(categoryService);
        }
    }
}
