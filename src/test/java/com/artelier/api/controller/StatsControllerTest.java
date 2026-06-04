package com.artelier.api.controller;

import com.artelier.api.dto.response.StatsResponse;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatsController.class)
@AutoConfigureMockMvc(addFilters = false)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsService statsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    @Test
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
}