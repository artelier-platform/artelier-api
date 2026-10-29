package com.artelier.api.controller;

import com.artelier.api.config.JacksonTestConfig;
import com.artelier.api.config.SecurityConfig;
import com.artelier.api.dto.request.OrderRequest;
import com.artelier.api.dto.response.OrderItemResponse;
import com.artelier.api.dto.response.OrderResponse;
import com.artelier.api.entity.User;
import com.artelier.api.enums.OrderStatus;
import com.artelier.api.enums.Role;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.security.UserPrincipal;
import com.artelier.api.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link OrderController} with the real {@link SecurityConfig} (filters enabled).
 * Authenticated calls use a real {@link UserPrincipal}, because the controller resolves it with
 * {@code @AuthenticationPrincipal}.
 */
@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, JacksonTestConfig.class})
@DisplayName("OrderController")
class OrderControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-05-04T15:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    private final UserPrincipal buyer = principal(Role.BUYER, "buyer@artelier.com");
    private final UserPrincipal admin = principal(Role.ADMIN, "admin@artelier.com");

    // ------------------------------------------------------------------ helpers

    private static UserPrincipal principal(Role role, String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setRole(role);
        return new UserPrincipal(user);
    }

    private static OrderResponse order(UUID id, OrderStatus status) {
        return OrderResponse.builder()
                .id(id)
                .status(status)
                .subtotal(new BigDecimal("89.97"))
                .total(new BigDecimal("89.97"))
                .shippingAddress("Cra 123 #45-67, Bogotá, Colombia")
                .notes("Please deliver after 5 PM")
                .createdAt(CREATED_AT)
                .items(List.of(new OrderItemResponse(
                        UUID.randomUUID(), "Handmade Ceramic Mug", 3, new BigDecimal("29.99"), "Gift wrap")))
                .build();
    }

    private static String itemJson(String productId, int quantity) {
        return """
                {"productId": %s, "quantity": %d, "customNotes": "Gift wrap"}
                """.formatted(productId, quantity);
    }

    private static String bodyJson(UUID productId) {
        return """
                {
                  "items": [{"productId": "%s", "quantity": 2, "customNotes": "Gift wrap this one"}],
                  "shippingAddress": "Cra 123 #45-67, Bogotá, Colombia",
                  "notes": "Please deliver after 5 PM"
                }
                """.formatted(productId);
    }

    record InvalidBody(String label, String json, String field) {
        @Override
        public String toString() {
            return label;
        }
    }

    static Stream<InvalidBody> invalidOrderBodies() {
        String validItem = itemJson("\"" + UUID.randomUUID() + "\"", 1);
        return Stream.of(
                new InvalidBody("items missing",
                        "{\"shippingAddress\": \"Cra 1\"}", "items"),
                new InvalidBody("items empty",
                        "{\"items\": [], \"shippingAddress\": \"Cra 1\"}", "items"),
                new InvalidBody("shippingAddress missing",
                        "{\"items\": [" + validItem + "]}", "shippingAddress"),
                new InvalidBody("shippingAddress blank",
                        "{\"items\": [" + validItem + "], \"shippingAddress\": \"   \"}", "shippingAddress"),
                new InvalidBody("item quantity zero",
                        "{\"items\": [" + itemJson("\"" + UUID.randomUUID() + "\"", 0)
                                + "], \"shippingAddress\": \"Cra 1\"}", "items[0].quantity"),
                new InvalidBody("item quantity negative",
                        "{\"items\": [" + itemJson("\"" + UUID.randomUUID() + "\"", -3)
                                + "], \"shippingAddress\": \"Cra 1\"}", "items[0].quantity"),
                new InvalidBody("item productId missing",
                        "{\"items\": [{\"quantity\": 1}], \"shippingAddress\": \"Cra 1\"}", "items[0].productId")
        );
    }

    // ------------------------------------------------------------------ POST /orders

    @Nested
    @DisplayName("POST /orders")
    class CreateOrder {

        @Test
        @DisplayName("200 – creates the order for the authenticated user and maps the response")
        void success() throws Exception {
            UUID productId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            when(orderService.createOrder(any(), eq("buyer@artelier.com")))
                    .thenReturn(order(orderId, OrderStatus.PENDING_PAYMENT));

            mockMvc.perform(post("/orders")
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyJson(productId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Order created successfully"))
                    .andExpect(jsonPath("$.data.id").value(orderId.toString()))
                    .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                    .andExpect(jsonPath("$.data.subtotal").value(89.97))
                    .andExpect(jsonPath("$.data.total").value(89.97))
                    .andExpect(jsonPath("$.data.shippingAddress").value("Cra 123 #45-67, Bogotá, Colombia"))
                    .andExpect(jsonPath("$.data.notes").value("Please deliver after 5 PM"))
                    .andExpect(jsonPath("$.data.createdAt").value("2026-05-04T15:30:00Z"))
                    .andExpect(jsonPath("$.data.items.length()").value(1))
                    .andExpect(jsonPath("$.data.items[0].productName").value("Handmade Ceramic Mug"))
                    .andExpect(jsonPath("$.data.items[0].quantity").value(3))
                    .andExpect(jsonPath("$.data.items[0].unitPrice").value(29.99))
                    .andExpect(jsonPath("$.data.items[0].customNotes").value("Gift wrap"));

            ArgumentCaptor<OrderRequest> captor = ArgumentCaptor.forClass(OrderRequest.class);
            verify(orderService).createOrder(captor.capture(), eq("buyer@artelier.com"));
            OrderRequest received = captor.getValue();
            assertThat(received.shippingAddress()).isEqualTo("Cra 123 #45-67, Bogotá, Colombia");
            assertThat(received.notes()).isEqualTo("Please deliver after 5 PM");
            assertThat(received.items()).hasSize(1);
            assertThat(received.items().getFirst().productId()).isEqualTo(productId);
            assertThat(received.items().getFirst().quantity()).isEqualTo(2);
            assertThat(received.items().getFirst().customNotes()).isEqualTo("Gift wrap this one");
        }

        @Test
        @DisplayName("200 – notes and customNotes are optional")
        void optionalFields() throws Exception {
            String json = """
                    {
                      "items": [{"productId": "%s", "quantity": 1}],
                      "shippingAddress": "Cra 1"
                    }
                    """.formatted(UUID.randomUUID());
            when(orderService.createOrder(any(), any())).thenReturn(order(UUID.randomUUID(), OrderStatus.PENDING_PAYMENT));

            mockMvc.perform(post("/orders")
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk());

            ArgumentCaptor<OrderRequest> captor = ArgumentCaptor.forClass(OrderRequest.class);
            verify(orderService).createOrder(captor.capture(), any());
            assertThat(captor.getValue().notes()).isNull();
            assertThat(captor.getValue().items().getFirst().customNotes()).isNull();
        }

        @ParameterizedTest(name = "400 – {0}")
        @MethodSource("com.artelier.api.controller.OrderControllerTest#invalidOrderBodies")
        @DisplayName("400 – bean validation failures report the offending field")
        void validationFailures(InvalidBody invalid) throws Exception {
            mockMvc.perform(post("/orders")
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalid.json()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.data[?(@.field == '" + invalid.field() + "')]").isNotEmpty());

            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("400 – malformed JSON")
        void malformedJson() throws Exception {
            mockMvc.perform(post("/orders")
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ not-json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid request body. Please check JSON format."));

            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("400 – productId that is not a UUID")
        void productIdNotAUuid() throws Exception {
            String json = "{\"items\": [" + itemJson("\"not-a-uuid\"", 1)
                    + "], \"shippingAddress\": \"Cra 1\"}";

            mockMvc.perform(post("/orders")
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("400 – insufficient stock reported by the service")
        void insufficientStock() throws Exception {
            when(orderService.createOrder(any(), any()))
                    .thenThrow(ArtelierException.badRequest("Insufficient stock"));

            mockMvc.perform(post("/orders")
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyJson(UUID.randomUUID())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Insufficient stock"));
        }

        @Test
        @DisplayName("404 – product does not exist")
        void productNotFound() throws Exception {
            when(orderService.createOrder(any(), any()))
                    .thenThrow(ArtelierException.notFound("Product not found"));

            mockMvc.perform(post("/orders")
                            .with(user(buyer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyJson(UUID.randomUUID())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Product not found"));
        }

        @Test
        @DisplayName("401 – anonymous request never reaches the service")
        void anonymousRejected() throws Exception {
            mockMvc.perform(post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyJson(UUID.randomUUID())))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(orderService);
        }
    }

    // ------------------------------------------------------------------ GET /orders/my

    @Nested
    @DisplayName("GET /orders/my")
    class MyOrders {

        @Test
        @DisplayName("200 – returns the orders of the authenticated user")
        void success() throws Exception {
            UUID first = UUID.randomUUID();
            UUID second = UUID.randomUUID();
            when(orderService.getMyOrders("buyer@artelier.com"))
                    .thenReturn(List.of(order(first, OrderStatus.PAID), order(second, OrderStatus.SHIPPED)));

            mockMvc.perform(get("/orders/my").with(user(buyer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Orders fetched"))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value(first.toString()))
                    .andExpect(jsonPath("$.data[0].status").value("PAID"))
                    .andExpect(jsonPath("$.data[1].id").value(second.toString()))
                    .andExpect(jsonPath("$.data[1].status").value("SHIPPED"));

            verify(orderService).getMyOrders("buyer@artelier.com");
        }

        @Test
        @DisplayName("200 – ADMIN also receives only the orders of their own account")
        void adminGetsOwnOrders() throws Exception {
            when(orderService.getMyOrders("admin@artelier.com")).thenReturn(List.of());

            mockMvc.perform(get("/orders/my").with(user(admin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(orderService).getMyOrders("admin@artelier.com");
        }

        @Test
        @DisplayName("200 – no orders returns an empty list")
        void empty() throws Exception {
            when(orderService.getMyOrders(any())).thenReturn(List.of());

            mockMvc.perform(get("/orders/my").with(user(buyer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("401 – anonymous request never reaches the service")
        void anonymousRejected() throws Exception {
            mockMvc.perform(get("/orders/my"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(orderService);
        }
    }

    // ------------------------------------------------------------------ GET /orders

    @Nested
    @DisplayName("GET /orders")
    class ListOrders {

        @Test
        @DisplayName("200 – BUYER: no filter, passes their own user to the service and maps the page")
        void buyerNoFilter() throws Exception {
            UUID id = UUID.randomUUID();
            when(orderService.getAllOrders(any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(order(id, OrderStatus.PAID)), PageRequest.of(0, 10), 25));

            mockMvc.perform(get("/orders").with(user(buyer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Orders fetched"))
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].id").value(id.toString()))
                    .andExpect(jsonPath("$.data.totalElements").value(25));

            verify(orderService).getAllOrders(isNull(), any(Pageable.class), same(buyer.getUser()));
        }

        @Test
        @DisplayName("200 – ADMIN: passes the admin user so the service can widen the result")
        void adminPassesAdminUser() throws Exception {
            when(orderService.getAllOrders(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/orders").with(user(admin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(0));

            verify(orderService).getAllOrders(isNull(), any(Pageable.class), same(admin.getUser()));
        }

        @Test
        @DisplayName("200 – status filter is converted to the enum")
        void statusFilter() throws Exception {
            when(orderService.getAllOrders(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/orders").param("status", "PAID").with(user(admin)))
                    .andExpect(status().isOk());

            verify(orderService).getAllOrders(eq(OrderStatus.PAID), any(Pageable.class), same(admin.getUser()));
        }

        @Test
        @DisplayName("200 – forwards page, size and sort to the service")
        void forwardsPagination() throws Exception {
            when(orderService.getAllOrders(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/orders")
                            .param("page", "2")
                            .param("size", "5")
                            .param("sort", "createdAt,desc")
                            .with(user(admin)))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(orderService).getAllOrders(isNull(), captor.capture(), any());
            Pageable pageable = captor.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(2);
            assertThat(pageable.getPageSize()).isEqualTo(5);
            assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
            assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
                    .isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("400 – unknown status value is rejected before reaching the service")
        void unknownStatus() throws Exception {
            mockMvc.perform(get("/orders").param("status", "UNKNOWN").with(user(admin)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid value for parameter 'status'"));

            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("401 – anonymous request never reaches the service")
        void anonymousRejected() throws Exception {
            mockMvc.perform(get("/orders"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(orderService);
        }
    }

    // ------------------------------------------------------------------ PATCH /orders/{id}/status

    @Nested
    @DisplayName("PATCH /orders/{id}/status")
    class UpdateStatus {

        @Test
        @DisplayName("200 – ADMIN ships a paid order")
        void adminShips() throws Exception {
            UUID id = UUID.randomUUID();
            when(orderService.updateOrderStatus(eq(id), eq(OrderStatus.SHIPPED), any()))
                    .thenReturn(order(id, OrderStatus.SHIPPED));

            mockMvc.perform(patch("/orders/{id}/status", id)
                            .param("status", "SHIPPED")
                            .with(user(admin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Order status updated"))
                    .andExpect(jsonPath("$.data.id").value(id.toString()))
                    .andExpect(jsonPath("$.data.status").value("SHIPPED"));

            verify(orderService).updateOrderStatus(eq(id), eq(OrderStatus.SHIPPED), same(admin.getUser()));
        }

        @Test
        @DisplayName("200 – BUYER cancels their own order")
        void buyerCancels() throws Exception {
            UUID id = UUID.randomUUID();
            when(orderService.updateOrderStatus(eq(id), eq(OrderStatus.CANCELLED), any()))
                    .thenReturn(order(id, OrderStatus.CANCELLED));

            mockMvc.perform(patch("/orders/{id}/status", id)
                            .param("status", "CANCELLED")
                            .with(user(buyer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"));

            verify(orderService).updateOrderStatus(eq(id), eq(OrderStatus.CANCELLED), same(buyer.getUser()));
        }

        @Test
        @DisplayName("400 – missing status parameter")
        void missingStatus() throws Exception {
            mockMvc.perform(patch("/orders/{id}/status", UUID.randomUUID()).with(user(admin)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Missing required parameter 'status'"));

            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("400 – unknown status value")
        void unknownStatus() throws Exception {
            mockMvc.perform(patch("/orders/{id}/status", UUID.randomUUID())
                            .param("status", "UNKNOWN")
                            .with(user(admin)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid value for parameter 'status'"));

            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("400 – order id that is not a UUID")
        void idNotAUuid() throws Exception {
            mockMvc.perform(patch("/orders/{id}/status", "not-a-uuid")
                            .param("status", "SHIPPED")
                            .with(user(admin)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));

            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("400 – transition not allowed by the service")
        void transitionNotAllowed() throws Exception {
            UUID id = UUID.randomUUID();
            when(orderService.updateOrderStatus(eq(id), any(), any()))
                    .thenThrow(ArtelierException.badRequest("Transition not allowed"));

            mockMvc.perform(patch("/orders/{id}/status", id)
                            .param("status", "PAID")
                            .with(user(admin)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Transition not allowed"));
        }

        @Test
        @DisplayName("403 – buyer modifying another user's order")
        void foreignOrder() throws Exception {
            UUID id = UUID.randomUUID();
            when(orderService.updateOrderStatus(eq(id), any(), any()))
                    .thenThrow(ArtelierException.forbidden("Access denied to this order"));

            mockMvc.perform(patch("/orders/{id}/status", id)
                            .param("status", "CANCELLED")
                            .with(user(buyer)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Access denied to this order"));
        }

        @Test
        @DisplayName("404 – order does not exist")
        void notFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(orderService.updateOrderStatus(eq(id), any(), any()))
                    .thenThrow(ArtelierException.notFound("Order not found"));

            mockMvc.perform(patch("/orders/{id}/status", id)
                            .param("status", "SHIPPED")
                            .with(user(admin)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Order not found"));
        }

        @Test
        @DisplayName("401 – anonymous request never reaches the service")
        void anonymousRejected() throws Exception {
            mockMvc.perform(patch("/orders/{id}/status", UUID.randomUUID()).param("status", "SHIPPED"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(orderService);
        }
    }

    // GET /orders/{id}

    @Nested
    @DisplayName("GET /orders/{id}")
    class GetById {

        @Test
        @DisplayName("200 – returns the order and passes the requester to the service")
        void success() throws Exception {
            UUID id = UUID.randomUUID();
            when(orderService.getOrderById(eq(id), any())).thenReturn(order(id, OrderStatus.PAID));

            mockMvc.perform(get("/orders/{id}", id).with(user(buyer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Order fetched"))
                    .andExpect(jsonPath("$.data.id").value(id.toString()))
                    .andExpect(jsonPath("$.data.status").value("PAID"));

            verify(orderService).getOrderById(eq(id), same(buyer.getUser()));
        }

        @Test
        @DisplayName("200 – ADMIN requests with the admin user")
        void adminRequester() throws Exception {
            UUID id = UUID.randomUUID();
            when(orderService.getOrderById(eq(id), any())).thenReturn(order(id, OrderStatus.SHIPPED));

            mockMvc.perform(get("/orders/{id}", id).with(user(admin)))
                    .andExpect(status().isOk());

            verify(orderService).getOrderById(eq(id), same(admin.getUser()));
        }

        @Test
        @DisplayName("400 – order id that is not a UUID")
        void idNotAUuid() throws Exception {
            mockMvc.perform(get("/orders/{id}", "not-a-uuid").with(user(buyer)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));

            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("403 – buyer requesting another user's order")
        void foreignOrder() throws Exception {
            UUID id = UUID.randomUUID();
            when(orderService.getOrderById(eq(id), any()))
                    .thenThrow(ArtelierException.forbidden("Access denied to this order"));

            mockMvc.perform(get("/orders/{id}", id).with(user(buyer)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Access denied to this order"));
        }

        @Test
        @DisplayName("404 – order does not exist")
        void notFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(orderService.getOrderById(eq(id), any()))
                    .thenThrow(ArtelierException.notFound("Order not found"));

            mockMvc.perform(get("/orders/{id}", id).with(user(buyer)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Order not found"));
        }

        @Test
        @DisplayName("401 – anonymous request never reaches the service")
        void anonymousRejected() throws Exception {
            mockMvc.perform(get("/orders/{id}", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(orderService);
        }
    }
}