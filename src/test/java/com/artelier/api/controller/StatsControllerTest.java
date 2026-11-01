package com.artelier.api.controller;

import com.artelier.api.config.WithSecurity;
import com.artelier.api.dto.response.StatsResponse;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.CustomAuthenticationEntryPoint;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatsController.class)
@WithSecurity
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsService statsService;


    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetStats() throws Exception {

        UUID productId = UUID.randomUUID();

        StatsResponse response = new StatsResponse(
                BigDecimal.valueOf(1500),
                5,
                12,
                new StatsResponse.TopProduct(
                        productId,
                        "Naruto Figure",
                        25
                )
        );

        when(statsService.getStats()).thenReturn(response);

        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSalesThisMonth").value(1500))
                .andExpect(jsonPath("$.data.pendingOrders").value(5))
                .andExpect(jsonPath("$.data.activeProducts").value(12))
                .andExpect(jsonPath("$.data.topSellingProduct.name").value("Naruto Figure"))
                .andExpect(jsonPath("$.data.topSellingProduct.totalSold").value(25));
    }

    @Test
    void anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(get("/admin/stats")).andExpect(status().isUnauthorized());
        verifyNoInteractions(statsService);
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void buyer_isForbidden() throws Exception {
        mockMvc.perform(get("/admin/stats")).andExpect(status().isForbidden());
        verifyNoInteractions(statsService);
    }
}