package com.artelier.api.service;

import com.artelier.api.dto.request.OrderRequest;
import com.artelier.api.dto.response.OrderResponse;
import com.artelier.api.entity.*;
import com.artelier.api.enums.OrderStatus;
import com.artelier.api.enums.Role;
import com.artelier.api.enums.StockType;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.mapper.OrderMapper;
import com.artelier.api.repository.OrderRepository;
import com.artelier.api.repository.ProductRepository;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks
    private OrderServiceImpl service;

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private User buildUser(Role role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setRole(role);
        return user;
    }

    private Product buildProduct(StockType stockType, int stock) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setIsActive(true);
        product.setPrice(BigDecimal.valueOf(100));
        product.setStockType(stockType);
        product.setStockQuantity(stock);
        return product;
    }

    private OrderRequest buildRequest(UUID productId, int quantity) {
        OrderRequest.OrderItemRequest item = new OrderRequest.OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setCustomNotes("note");

        OrderRequest request = new OrderRequest();
        request.setShippingAddress("Calle 123");
        request.setNotes("Urgente");
        request.setItems(List.of(item));
        return request;
    }

    // ─────────────────────────────────────────────
    // createOrder
    // ─────────────────────────────────────────────

    @Test
    void shouldCreateOrderSuccessfully() {
        User user = buildUser(Role.BUYER);
        Product product = buildProduct(StockType.AVAILABLE, 10);
        OrderRequest request = buildRequest(product.getId(), 2);

        Order savedOrder = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING_PAYMENT)
                .items(new ArrayList<>())
                .build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(orderRepository.save(any())).thenReturn(savedOrder);
        when(orderMapper.toResponse(savedOrder)).thenReturn(OrderResponse.builder().build());

        OrderResponse result = service.createOrder(request, user.getEmail());

        assertNotNull(result);
        assertEquals(8, product.getStockQuantity()); // 10 - 2
        verify(orderRepository).save(any());
    }

    @Test
    void shouldCreateOrderWithUnlimitedStock() {
        User user = buildUser(Role.BUYER);
        Product product = buildProduct(StockType.UNLIMITED, 0);
        OrderRequest request = buildRequest(product.getId(), 99);

        Order savedOrder = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING_PAYMENT)
                .items(new ArrayList<>())
                .build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(orderRepository.save(any())).thenReturn(savedOrder);
        when(orderMapper.toResponse(savedOrder)).thenReturn(OrderResponse.builder().build());

        OrderResponse result = service.createOrder(request, user.getEmail());

        assertNotNull(result);
        // stock quantity untouched for UNLIMITED
        assertEquals(0, product.getStockQuantity());
    }

    @Test
    void shouldThrowIfInsufficientStock() {
        User user = buildUser(Role.BUYER);
        Product product = buildProduct(StockType.AVAILABLE, 1);
        OrderRequest request = buildRequest(product.getId(), 5);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThrows(ArtelierException.class,
                () -> service.createOrder(request, user.getEmail()));
    }

    @Test
    void shouldThrowIfUserNotFoundOnCreate() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        OrderRequest request = new OrderRequest();
        request.setItems(List.of());

        assertThrows(ArtelierException.class,
                () -> service.createOrder(request, "ghost@test.com"));
    }

    @Test
    void shouldThrowIfProductNotFoundOnCreate() {
        User user = buildUser(Role.BUYER);
        OrderRequest request = buildRequest(UUID.randomUUID(), 1);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(productRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.createOrder(request, user.getEmail()));
    }

    @Test
    void shouldThrowIfProductIsInactiveOnCreate() {
        User user = buildUser(Role.BUYER);
        Product product = buildProduct(StockType.AVAILABLE, 10);
        product.setIsActive(false);
        OrderRequest request = buildRequest(product.getId(), 1);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThrows(ArtelierException.class,
                () -> service.createOrder(request, user.getEmail()));
    }

    // ─────────────────────────────────────────────
    // getMyOrders
    // ─────────────────────────────────────────────

    @Test
    void shouldReturnMyOrders() {
        String email = "user@test.com";
        Order order = new Order();

        when(orderRepository.findByUserEmailOrderByCreatedAtDesc(email))
                .thenReturn(List.of(order));
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().build());

        List<OrderResponse> result = service.getMyOrders(email);

        assertEquals(1, result.size());
        verify(orderRepository).findByUserEmailOrderByCreatedAtDesc(email);
    }

    @Test
    void shouldReturnEmptyListIfNoOrders() {
        String email = "user@test.com";

        when(orderRepository.findByUserEmailOrderByCreatedAtDesc(email))
                .thenReturn(List.of());

        List<OrderResponse> result = service.getMyOrders(email);

        assertTrue(result.isEmpty());
    }

    // ─────────────────────────────────────────────
    // getAllOrders — ADMIN paths
    // ─────────────────────────────────────────────

    @Test
    void adminShouldGetAllOrdersWithoutStatusFilter() {
        User admin = buildUser(Role.ADMIN);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(new Order()));

        when(orderRepository.findAll(pageable)).thenReturn(page);
        when(orderMapper.toResponse(any())).thenReturn(OrderResponse.builder().build());

        Page<OrderResponse> result = service.getAllOrders(null, pageable, admin);

        assertEquals(1, result.getContent().size());
        verify(orderRepository).findAll(pageable);
        verify(orderRepository, never()).findByStatus(any(), any());
    }

    @Test
    void adminShouldGetAllOrdersFilteredByStatus() {
        User admin = buildUser(Role.ADMIN);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(new Order()));

        when(orderRepository.findByStatus(OrderStatus.PAID, pageable)).thenReturn(page);
        when(orderMapper.toResponse(any())).thenReturn(OrderResponse.builder().build());

        Page<OrderResponse> result = service.getAllOrders(OrderStatus.PAID, pageable, admin);

        assertEquals(1, result.getContent().size());
        verify(orderRepository).findByStatus(OrderStatus.PAID, pageable);
        verify(orderRepository, never()).findAll(pageable);
    }

    // ─────────────────────────────────────────────
    // getAllOrders — BUYER paths
    // ─────────────────────────────────────────────

    @Test
    void buyerShouldGetOnlyOwnOrdersWithoutStatusFilter() {
        User buyer = buildUser(Role.BUYER);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(new Order()));

        when(orderRepository.findByUser(buyer, pageable)).thenReturn(page);
        when(orderMapper.toResponse(any())).thenReturn(OrderResponse.builder().build());

        Page<OrderResponse> result = service.getAllOrders(null, pageable, buyer);

        assertEquals(1, result.getContent().size());
        verify(orderRepository).findByUser(buyer, pageable);
        verify(orderRepository, never()).findAll(pageable);
    }

    @Test
    void buyerShouldGetOnlyOwnOrdersFilteredByStatus() {
        User buyer = buildUser(Role.BUYER);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(new Order()));

        when(orderRepository.findByUserAndStatus(buyer, OrderStatus.PENDING_PAYMENT, pageable))
                .thenReturn(page);
        when(orderMapper.toResponse(any())).thenReturn(OrderResponse.builder().build());

        Page<OrderResponse> result = service.getAllOrders(OrderStatus.PENDING_PAYMENT, pageable, buyer);

        assertEquals(1, result.getContent().size());
        verify(orderRepository).findByUserAndStatus(buyer, OrderStatus.PENDING_PAYMENT, pageable);
    }

    // ─────────────────────────────────────────────
    // updateOrderStatus — ADMIN paths
    // ─────────────────────────────────────────────

    @Test
    void adminShouldShipPaidOrder() {
        User admin = buildUser(Role.ADMIN);
        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setStatus(OrderStatus.PAID);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().build());

        service.updateOrderStatus(orderId, OrderStatus.SHIPPED, admin);

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
    }

    @Test
    void adminShouldThrowIfTargetStatusIsNotShipped() {
        User admin = buildUser(Role.ADMIN);
        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setStatus(OrderStatus.PAID);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(ArtelierException.class,
                () -> service.updateOrderStatus(orderId, OrderStatus.CANCELLED, admin));
    }

    @Test
    void adminShouldThrowIfOrderIsNotPaidWhenShipping() {
        User admin = buildUser(Role.ADMIN);
        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setStatus(OrderStatus.PROCESSING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(ArtelierException.class,
                () -> service.updateOrderStatus(orderId, OrderStatus.SHIPPED, admin));
    }

    // ─────────────────────────────────────────────
    // updateOrderStatus — BUYER paths
    // ─────────────────────────────────────────────

    @Test
    void buyerShouldCancelOwnPendingOrder() {
        User buyer = buildUser(Role.BUYER);
        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setUser(buyer);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().build());

        service.updateOrderStatus(orderId, OrderStatus.CANCELLED, buyer);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void buyerShouldThrowIfTargetStatusIsNotCancelled() {
        User buyer = buildUser(Role.BUYER);
        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setUser(buyer);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(ArtelierException.class,
                () -> service.updateOrderStatus(orderId, OrderStatus.SHIPPED, buyer));
    }

    @Test
    void buyerShouldThrowIfOrderIsNotPendingPaymentWhenCancelling() {
        User buyer = buildUser(Role.BUYER);
        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setStatus(OrderStatus.PAID);
        order.setUser(buyer);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(ArtelierException.class,
                () -> service.updateOrderStatus(orderId, OrderStatus.CANCELLED, buyer));
    }

    @Test
    void buyerShouldThrowIfOrderBelongsToAnotherUser() {
        User buyer = buildUser(Role.BUYER);

        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());

        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setUser(otherUser);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(ArtelierException.class,
                () -> service.updateOrderStatus(orderId, OrderStatus.CANCELLED, buyer));
    }

    @Test
    void shouldThrowIfOrderNotFoundOnUpdateStatus() {
        User admin = buildUser(Role.ADMIN);
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.updateOrderStatus(UUID.randomUUID(), OrderStatus.SHIPPED, admin));
    }

    // ─────────────────────────────────────────────
    // updateOrderStatusInternal
    // ─────────────────────────────────────────────

    @Test
    void shouldUpdateOrderStatusInternally() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setStatus(OrderStatus.PROCESSING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        service.updateOrderStatusInternal(orderId, OrderStatus.PAID);

        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldThrowInternalUpdateIfOrderIsCancelled() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(ArtelierException.class,
                () -> service.updateOrderStatusInternal(orderId, OrderStatus.PAID));
    }

    @Test
    void shouldThrowIfOrderNotFoundOnInternalUpdate() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.updateOrderStatusInternal(UUID.randomUUID(), OrderStatus.PAID));
    }

    // ─────────────────────────────────────────────
    // getOrderById
    // ─────────────────────────────────────────────

    @Test
    void adminShouldGetAnyOrderById() {
        User admin = buildUser(Role.ADMIN);
        UUID orderId = UUID.randomUUID();

        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());

        Order order = new Order();
        order.setUser(otherUser);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().build());

        OrderResponse result = service.getOrderById(orderId, admin);

        assertNotNull(result);
    }

    @Test
    void buyerShouldGetOwnOrderById() {
        User buyer = buildUser(Role.BUYER);
        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setUser(buyer);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().build());

        OrderResponse result = service.getOrderById(orderId, buyer);

        assertNotNull(result);
    }

    @Test
    void buyerShouldThrowIfAccessingAnotherUsersOrder() {
        User buyer = buildUser(Role.BUYER);

        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());

        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setUser(otherUser);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(ArtelierException.class,
                () -> service.getOrderById(orderId, buyer));
    }

    @Test
    void shouldThrowIfOrderNotFoundById() {
        User admin = buildUser(Role.ADMIN);
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.getOrderById(UUID.randomUUID(), admin));
    }
}