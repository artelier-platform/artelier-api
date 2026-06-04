package com.artelier.api.controller;

import com.artelier.api.config.JacksonTestConfig;
import com.artelier.api.dto.request.OrderRequest;
import com.artelier.api.dto.response.OrderResponse;
import com.artelier.api.entity.User;
import com.artelier.api.enums.OrderStatus;
import com.artelier.api.enums.Role;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.security.UserPrincipal;
import com.artelier.api.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @MockitoBean
    private UserRepository userRepository;

    private static final String USER_EMAIL = "user@test.com";

    private User buyerUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        buyerUser = new User();
        buyerUser.setId(UUID.randomUUID());
        buyerUser.setEmail(USER_EMAIL);
        buyerUser.setRole(Role.BUYER);

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(Role.ADMIN);
    }

    private void authenticateAs(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
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

    @Test
    void shouldCreateOrder() throws Exception {
        authenticateAs(buyerUser);

        OrderResponse response = OrderResponse.builder()
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        when(orderService.createOrder(any(), eq(USER_EMAIL))).thenReturn(response);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order created successfully"))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));
    }

    @Test
    void shouldGetMyOrders() throws Exception {
        authenticateAs(buyerUser);

        OrderResponse order = OrderResponse.builder()
                .status(OrderStatus.PAID)
                .build();

        when(orderService.getMyOrders(USER_EMAIL)).thenReturn(List.of(order));

        mockMvc.perform(get("/orders/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].status").value("PAID"));
    }

    @Test
    void shouldReturnEmptyListIfNoOrders() throws Exception {
        authenticateAs(buyerUser);

        when(orderService.getMyOrders(USER_EMAIL)).thenReturn(List.of());

        mockMvc.perform(get("/orders/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void shouldGetAllOrdersWithoutFilter() throws Exception {
        authenticateAs(adminUser);

        OrderResponse order = OrderResponse.builder().build();

        when(orderService.getAllOrders(isNull(), any(), eq(adminUser)))
                .thenReturn(new PageImpl<>(List.of(order)));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void shouldGetAllOrdersFilteredByStatus() throws Exception {
        authenticateAs(adminUser);

        OrderResponse order = OrderResponse.builder()
                .status(OrderStatus.SHIPPED)
                .build();

        when(orderService.getAllOrders(eq(OrderStatus.SHIPPED), any(), eq(adminUser)))
                .thenReturn(new PageImpl<>(List.of(order)));

        mockMvc.perform(get("/orders").param("status", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("SHIPPED"));
    }

    @Test
    void adminShouldUpdateOrderStatusToShipped() throws Exception {
        authenticateAs(adminUser);
        UUID orderId = UUID.randomUUID();

        OrderResponse response = OrderResponse.builder()
                .status(OrderStatus.SHIPPED)
                .build();

        when(orderService.updateOrderStatus(orderId, OrderStatus.SHIPPED, adminUser))
                .thenReturn(response);

        mockMvc.perform(patch("/orders/" + orderId + "/status")
                        .param("status", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order status updated"))
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));
    }

    @Test
    void buyerShouldCancelOwnOrder() throws Exception {
        authenticateAs(buyerUser);
        UUID orderId = UUID.randomUUID();

        OrderResponse response = OrderResponse.builder()
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED, buyerUser))
                .thenReturn(response);

        mockMvc.perform(patch("/orders/" + orderId + "/status")
                        .param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void shouldGetOrderById() throws Exception {
        authenticateAs(adminUser);
        UUID orderId = UUID.randomUUID();

        OrderResponse response = OrderResponse.builder()
                .status(OrderStatus.PAID)
                .build();

        when(orderService.getOrderById(orderId, adminUser)).thenReturn(response);

        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order fetched"))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    void buyerShouldGetOwnOrderById() throws Exception {
        authenticateAs(buyerUser);
        UUID orderId = UUID.randomUUID();

        OrderResponse response = OrderResponse.builder()
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        when(orderService.getOrderById(orderId, buyerUser)).thenReturn(response);

        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));
    }
}