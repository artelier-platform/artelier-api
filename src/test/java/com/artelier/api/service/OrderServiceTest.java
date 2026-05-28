package com.artelier.api.service;

import com.artelier.api.dto.request.OrderRequest;
import com.artelier.api.dto.response.OrderResponse;
import com.artelier.api.entity.*;
import com.artelier.api.entity.enums.OrderStatus;
import com.artelier.api.entity.enums.StockType;
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


    @Test
    void shouldCreateOrderSuccessfully() {
        String email = "user@test.com";

        User user = new User();
        user.setEmail(email);

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setIsActive(true);
        product.setPrice(BigDecimal.valueOf(100));
        product.setStockType(StockType.AVAILABLE);
        product.setStockQuantity(10);

        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest();
        itemRequest.setProductId(product.getId());
        itemRequest.setQuantity(2);
        itemRequest.setCustomNotes("note");

        OrderRequest request = new OrderRequest();
        request.setShippingAddress("Calle 123");
        request.setNotes("Urgente");
        request.setItems(List.of(itemRequest));

        Order savedOrder = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING_PAYMENT)
                .items(new ArrayList<>())
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(orderRepository.save(any())).thenReturn(savedOrder);
        when(orderMapper.toResponse(savedOrder)).thenReturn(OrderResponse.builder().build());

        OrderResponse result = service.createOrder(request, email);

        assertNotNull(result);
        verify(orderRepository).save(any());
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
        String email = "user@test.com";
        User user = new User();

        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest();
        itemRequest.setProductId(UUID.randomUUID());
        itemRequest.setQuantity(1);

        OrderRequest request = new OrderRequest();
        request.setShippingAddress("Addr");
        request.setItems(List.of(itemRequest));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(productRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.createOrder(request, email));
    }

    @Test
    void shouldThrowIfProductIsInactiveOnCreate() {
        String email = "user@test.com";
        User user = new User();

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setIsActive(false);
        product.setPrice(BigDecimal.TEN);

        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest();
        itemRequest.setProductId(product.getId());
        itemRequest.setQuantity(1);

        OrderRequest request = new OrderRequest();
        request.setShippingAddress("Addr");
        request.setItems(List.of(itemRequest));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThrows(ArtelierException.class,
                () -> service.createOrder(request, email));
    }


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
    void shouldReturnAllOrdersWithoutStatusFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(new Order()));

        when(orderRepository.findAll(pageable)).thenReturn(page);
        when(orderMapper.toResponse(any())).thenReturn(OrderResponse.builder().build());

        Page<OrderResponse> result = service.getAllOrders(null, pageable);

        assertEquals(1, result.getContent().size());
        verify(orderRepository).findAll(pageable);
    }

    @Test
    void shouldReturnAllOrdersFilteredByStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(new Order()));

        when(orderRepository.findByStatus(OrderStatus.PENDING_PAYMENT, pageable)).thenReturn(page);
        when(orderMapper.toResponse(any())).thenReturn(OrderResponse.builder().build());

        Page<OrderResponse> result = service.getAllOrders(OrderStatus.PENDING_PAYMENT, pageable);

        assertEquals(1, result.getContent().size());
        verify(orderRepository).findByStatus(OrderStatus.PENDING_PAYMENT, pageable);
    }


    @Test
    void shouldUpdateOrderStatus() {
        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().build());

        OrderResponse result = service.updateOrderStatus(orderId, OrderStatus.SHIPPED);

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertNotNull(result);
    }

    @Test
    void shouldThrowIfOrderNotFoundOnUpdateStatus() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.updateOrderStatus(UUID.randomUUID(), OrderStatus.SHIPPED));
    }

    @Test
    void shouldThrowIfOrderIsCancelledOnUpdateStatus() {
        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(ArtelierException.class,
                () -> service.updateOrderStatus(orderId, OrderStatus.SHIPPED));
    }


    @Test
    void shouldReturnOrderById() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().build());

        OrderResponse result = service.getOrderById(orderId);

        assertNotNull(result);
    }

    @Test
    void shouldThrowIfOrderNotFoundById() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.getOrderById(UUID.randomUUID()));
    }
}