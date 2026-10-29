package com.artelier.api.controller;

import com.artelier.api.config.JacksonTestConfig;
import com.artelier.api.config.SecurityConfig;
import com.artelier.api.dto.response.BannerResponse;
import com.artelier.api.dto.response.ItemBannerResponse;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.BannerService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Access rules of {@code GET /banners} with the real {@link SecurityConfig} (filters enabled).
 */
@WebMvcTest(BannerController.class)
@Import({SecurityConfig.class, JacksonTestConfig.class})
@DisplayName("BannerController – security")
class BannerControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BannerService bannerService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    private void stubOneBanner() {
        ItemBannerResponse item = new ItemBannerResponse(
                UUID.randomUUID(), true, 1, "Summer", true,
                "https://res.cloudinary.com/artelier/image/upload/bg.jpg", "artelier/banners/bg",
                null, null, null, false, null, null, false);
        when(bannerService.getActiveBanners())
                .thenReturn(List.of(new BannerResponse(
                        UUID.randomUUID(), true, Instant.parse("2026-10-27T10:00:00Z"), List.of(item))));
    }

    /**
     * The controller documents GET /banners as public ("No authentication is required"), but
     * SecurityConfig has no matcher for it, so {@code anyRequest().authenticated()} answers 401
     * to anonymous visitors. Enable this test after adding
     * {@code .requestMatchers(HttpMethod.GET, "/banners").permitAll()} to SecurityConfig.
     */
    @Test
    @DisplayName("200 – anonymous visitor can list active banners (documented as public)")
    void anonymousCanListBanners() throws Exception {
        stubOneBanner();

        mockMvc.perform(get("/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bannerService).getActiveBanners();
    }

    @Test
    @DisplayName("200 – authenticated BUYER can list active banners")
    @WithMockUser(roles = "BUYER")
    void buyerCanListBanners() throws Exception {
        stubOneBanner();

        mockMvc.perform(get("/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        verify(bannerService).getActiveBanners();
    }

    @Test
    @DisplayName("200 – authenticated ADMIN can list active banners")
    @WithMockUser(roles = "ADMIN")
    void adminCanListBanners() throws Exception {
        stubOneBanner();

        mockMvc.perform(get("/banners"))
                .andExpect(status().isOk());
    }
}
