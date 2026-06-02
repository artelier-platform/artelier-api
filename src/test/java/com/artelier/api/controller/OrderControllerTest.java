package com.artelier.api.controller;

import com.artelier.api.config.JacksonTestConfig;
import com.artelier.api.dto.request.OrderRequest;
import com.artelier.api.dto.response.OrderResponse;
import com.artelier.api.enums.OrderStatus;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonTestConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String AUTH_HEADER = "Bearer fake-token";
    private static final String USER_EMAIL  = "user@test.com";


    @Test
    void shouldCreateOrder() throws Exception {
        OrderRequest request = buildRequest();

        OrderResponse response = OrderResponse.builder()
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        when(jwtUtil.extractUsername("fake-token")).thenReturn(USER_EMAIL);
        when(orderService.createOrder(any(), eq(USER_EMAIL))).thenReturn(response);

        mockMvc.perform(post("/orders")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order created successfully"));
    }


    @Test
    void shouldGetMyOrders() throws Exception {
        OrderResponse order = OrderResponse.builder()
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        when(jwtUtil.extractUsername("fake-token")).thenReturn(USER_EMAIL);
        when(orderService.getMyOrders(USER_EMAIL)).thenReturn(List.of(order));

        mockMvc.perform(get("/orders/my")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].status").value("PENDING_PAYMENT"));
    }


    @Test
    void shouldGetAllOrdersWithoutFilter() throws Exception {
        OrderResponse order = OrderResponse.builder().build();

        when(orderService.getAllOrders(isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(order)));

        mockMvc.perform(get("/orders/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void shouldGetAllOrdersFilteredByStatus() throws Exception {
        OrderResponse order = OrderResponse.builder()
                .status(OrderStatus.SHIPPED)
                .build();

        when(orderService.getAllOrders(eq(OrderStatus.SHIPPED), any()))
                .thenReturn(new PageImpl<>(List.of(order)));

        mockMvc.perform(get("/orders/admin")
                        .param("status", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("SHIPPED"));
    }


    @Test
    void shouldUpdateOrderStatus() throws Exception {
        UUID orderId = UUID.randomUUID();

        OrderResponse response = OrderResponse.builder()
                .status(OrderStatus.SHIPPED)
                .build();

        when(orderService.updateOrderStatus(orderId, OrderStatus.SHIPPED)).thenReturn(response);

        mockMvc.perform(patch("/orders/admin/" + orderId + "/status")
                        .param("status", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order status updated"));
    }


    @Test
    void shouldGetOrderById() throws Exception {
        UUID orderId = UUID.randomUUID();

        OrderResponse response = OrderResponse.builder()
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        when(orderService.getOrderById(orderId)).thenReturn(response);

        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order fetched"));
    }


    private OrderRequest buildRequest() {
        OrderRequest.OrderItemRequest item = new OrderRequest.OrderItemRequest();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(2);
        item.setCustomNotes("sin cambios");

        OrderRequest request = new OrderRequest();
        request.setShippingAddress("Calle 123 #45-67");
        request.setNotes("Entregar en la tarde");
        request.setItems(List.of(item));

        return request;
    }
}