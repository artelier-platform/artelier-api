package com.artelier.api.service;

import com.artelier.api.dto.request.OrderRequest;
import com.artelier.api.dto.response.OrderResponse;
import com.artelier.api.entity.Order;
import com.artelier.api.entity.Product;
import com.artelier.api.entity.User;
import com.artelier.api.enums.OrderStatus;
import com.artelier.api.enums.Role;
import com.artelier.api.enums.StockType;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.mapper.OrderMapper;
import com.artelier.api.repository.OrderRepository;
import com.artelier.api.repository.ProductRepository;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OrderServiceImpl}. Repositories and mapper are mocked,
 * so no Spring context or database is involved.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl")
class OrderServiceTest {

    private static final String EMAIL = "buyer@artelier.com";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, orderMapper, userRepository, productRepository);
    }

    // ---------------------------------------------------------------- helpers

    private static User user(Role role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(role.name().toLowerCase() + "@artelier.com");
        user.setRole(role);
        return user;
    }

    private static Product product(String name, String price, StockType stockType, Integer stock, boolean active) {
        return Product.builder()
                .id(UUID.randomUUID())
                .name(name)
                .price(new BigDecimal(price))
                .stockType(stockType)
                .stockQuantity(stock)
                .isActive(active)
                .build();
    }

    private static Order order(User owner, OrderStatus status) {
        return Order.builder()
                .id(UUID.randomUUID())
                .user(owner)
                .status(status)
                .build();
    }

    private static OrderResponse responseOf(Order order) {
        return OrderResponse.builder().id(order.getId()).status(order.getStatus()).build();
    }

    private static OrderRequest.OrderItemRequest item(Product product, int quantity, String notes) {
        return new OrderRequest.OrderItemRequest(product.getId(), quantity, notes);
    }

    private static void assertArtelier(HttpStatus status, String message, Executable action) {
        ArtelierException exception = assertThrows(ArtelierException.class, action);
        assertEquals(status, exception.getStatus());
        assertEquals(message, exception.getMessage());
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }

    // ------------------------------------------------------------ createOrder

    @Nested
    @DisplayName("createOrder(request, userEmail)")
    class CreateOrder {

        @Test
        @DisplayName("creates a PENDING_PAYMENT order with items, totals and decremented stock")
        void createsOrder() {
            User buyer = user(Role.BUYER);
            Product mug = product("Ceramic Mug", "29.99", StockType.AVAILABLE, 5, true);
            OrderRequest request = new OrderRequest(
                    List.of(item(mug, 2, "Gift wrap")), "Cra 123 #45-67", "After 5 PM");
            OrderResponse expected = OrderResponse.builder().id(UUID.randomUUID()).build();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buyer));
            when(productRepository.findById(mug.getId())).thenReturn(Optional.of(mug));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderMapper.toResponse(any(Order.class))).thenReturn(expected);

            OrderResponse result = orderService.createOrder(request, EMAIL);

            assertSame(expected, result);

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(captor.capture());
            Order persisted = captor.getValue();
            assertSame(buyer, persisted.getUser());
            assertEquals(OrderStatus.PENDING_PAYMENT, persisted.getStatus());
            assertEquals(EMAIL, persisted.getCustomerEmail());
            assertEquals("Cra 123 #45-67", persisted.getShippingAddress());
            assertEquals("After 5 PM", persisted.getNotes());
            assertAmount("59.98", persisted.getSubtotal());
            assertAmount("59.98", persisted.getTotal());

            assertEquals(1, persisted.getItems().size());
            var persistedItem = persisted.getItems().get(0);
            assertSame(persisted, persistedItem.getOrder());
            assertSame(mug, persistedItem.getProduct());
            assertEquals(2, persistedItem.getQuantity());
            assertAmount("29.99", persistedItem.getUnitPrice());
            assertEquals("Gift wrap", persistedItem.getCustomNotes());

            assertEquals(3, mug.getStockQuantity());
            verify(orderMapper).toResponse(persisted);
        }

        @Test
        @DisplayName("sums every line into subtotal and total")
        void sumsMultipleItems() {
            User buyer = user(Role.BUYER);
            Product mug = product("Mug", "10.00", StockType.AVAILABLE, 10, true);
            Product plate = product("Plate", "5.50", StockType.AVAILABLE, 10, true);
            OrderRequest request = new OrderRequest(
                    List.of(item(mug, 3, null), item(plate, 2, null)), "Address", null);

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buyer));
            when(productRepository.findById(mug.getId())).thenReturn(Optional.of(mug));
            when(productRepository.findById(plate.getId())).thenReturn(Optional.of(plate));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().build());

            orderService.createOrder(request, EMAIL);

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(captor.capture());
            Order persisted = captor.getValue();
            assertEquals(2, persisted.getItems().size());
            assertAmount("41.00", persisted.getSubtotal());
            assertAmount("41.00", persisted.getTotal());
            assertNull(persisted.getNotes());
            assertEquals(7, mug.getStockQuantity());
            assertEquals(8, plate.getStockQuantity());
        }

        @Test
        @DisplayName("UNLIMITED products skip the stock check and keep their stock untouched")
        void unlimitedStockIsNotChecked() {
            User buyer = user(Role.BUYER);
            Product scarf = product("Scarf", "20.00", StockType.UNLIMITED, null, true);
            OrderRequest request = new OrderRequest(List.of(item(scarf, 100, null)), "Address", null);

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buyer));
            when(productRepository.findById(scarf.getId())).thenReturn(Optional.of(scarf));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().build());

            orderService.createOrder(request, EMAIL);

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(captor.capture());
            assertAmount("2000.00", captor.getValue().getTotal());
            assertNull(scarf.getStockQuantity());
        }

        @Test
        @DisplayName("ordering exactly the available stock is allowed and leaves zero")
        void exactStockIsAllowed() {
            User buyer = user(Role.BUYER);
            Product vase = product("Vase", "40.00", StockType.AVAILABLE, 2, true);
            OrderRequest request = new OrderRequest(List.of(item(vase, 2, null)), "Address", null);

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buyer));
            when(productRepository.findById(vase.getId())).thenReturn(Optional.of(vase));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().build());

            orderService.createOrder(request, EMAIL);

            assertEquals(0, vase.getStockQuantity());
        }

        @Test
        @DisplayName("404 – unknown user: nothing else is touched")
        void userNotFound() {
            OrderRequest request = new OrderRequest(
                    List.of(new OrderRequest.OrderItemRequest(UUID.randomUUID(), 1, null)), "Address", null);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertArtelier(HttpStatus.NOT_FOUND, "User not found",
                    () -> orderService.createOrder(request, EMAIL));

            verifyNoInteractions(productRepository, orderRepository, orderMapper);
        }

        @Test
        @DisplayName("404 – unknown product: the order is not saved")
        void productNotFound() {
            User buyer = user(Role.BUYER);
            UUID missingId = UUID.randomUUID();
            OrderRequest request = new OrderRequest(
                    List.of(new OrderRequest.OrderItemRequest(missingId, 1, null)), "Address", null);

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buyer));
            when(productRepository.findById(missingId)).thenReturn(Optional.empty());

            assertArtelier(HttpStatus.NOT_FOUND, "Product not found",
                    () -> orderService.createOrder(request, EMAIL));

            verify(orderRepository, never()).save(any(Order.class));
            verifyNoInteractions(orderMapper);
        }

        @Test
        @DisplayName("400 – inactive product is rejected")
        void inactiveProduct() {
            User buyer = user(Role.BUYER);
            Product retired = product("Retired Bowl", "15.00", StockType.AVAILABLE, 5, false);
            OrderRequest request = new OrderRequest(List.of(item(retired, 1, null)), "Address", null);

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buyer));
            when(productRepository.findById(retired.getId())).thenReturn(Optional.of(retired));

            assertArtelier(HttpStatus.BAD_REQUEST, "Product is not available",
                    () -> orderService.createOrder(request, EMAIL));

            verify(orderRepository, never()).save(any(Order.class));
            verifyNoInteractions(orderMapper);
        }

        @Test
        @DisplayName("400 – not enough stock: message reports product and available units, nothing saved")
        void insufficientStock() {
            User buyer = user(Role.BUYER);
            Product mug = product("Ceramic Mug", "29.99", StockType.AVAILABLE, 1, true);
            OrderRequest request = new OrderRequest(List.of(item(mug, 2, null)), "Address", null);

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buyer));
            when(productRepository.findById(mug.getId())).thenReturn(Optional.of(mug));

            assertArtelier(HttpStatus.BAD_REQUEST,
                    "Insufficient stock for product: Ceramic Mug (available: 1)",
                    () -> orderService.createOrder(request, EMAIL));

            assertEquals(1, mug.getStockQuantity());
            verify(orderRepository, never()).save(any(Order.class));
            verifyNoInteractions(orderMapper);
        }

        @Test
        @DisplayName("400 – tracked stock with null quantity counts as zero available")
        void nullStockCountsAsZero() {
            User buyer = user(Role.BUYER);
            Product mug = product("Ceramic Mug", "29.99", StockType.AVAILABLE, null, true);
            OrderRequest request = new OrderRequest(List.of(item(mug, 1, null)), "Address", null);

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buyer));
            when(productRepository.findById(mug.getId())).thenReturn(Optional.of(mug));

            assertArtelier(HttpStatus.BAD_REQUEST,
                    "Insufficient stock for product: Ceramic Mug (available: 0)",
                    () -> orderService.createOrder(request, EMAIL));

            verify(orderRepository, never()).save(any(Order.class));
        }
    }

    // ------------------------------------------------------------- getMyOrders

    @Nested
    @DisplayName("getMyOrders(userEmail)")
    class GetMyOrders {

        @Test
        @DisplayName("maps the user's orders keeping the repository order")
        void mapsInOrder() {
            User buyer = user(Role.BUYER);
            Order newer = order(buyer, OrderStatus.PAID);
            Order older = order(buyer, OrderStatus.PENDING_PAYMENT);
            OrderResponse newerResponse = responseOf(newer);
            OrderResponse olderResponse = responseOf(older);

            when(orderRepository.findByUserEmailOrderByCreatedAtDesc(EMAIL)).thenReturn(List.of(newer, older));
            when(orderMapper.toResponse(newer)).thenReturn(newerResponse);
            when(orderMapper.toResponse(older)).thenReturn(olderResponse);

            assertEquals(List.of(newerResponse, olderResponse), orderService.getMyOrders(EMAIL));
        }

        @Test
        @DisplayName("returns an empty list when the user has no orders")
        void noOrders() {
            when(orderRepository.findByUserEmailOrderByCreatedAtDesc(EMAIL)).thenReturn(List.of());

            assertTrue(orderService.getMyOrders(EMAIL).isEmpty());
            verifyNoInteractions(orderMapper);
        }
    }

    // ------------------------------------------------------------- getAllOrders

    @Nested
    @DisplayName("getAllOrders(status, pageable, requester)")
    class GetAllOrders {

        private final Pageable pageable = PageRequest.of(0, 10);

        private void assertSingleMappedPage(Page<OrderResponse> result, OrderResponse expected) {
            assertEquals(List.of(expected), result.getContent());
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("ADMIN without status filter lists every order")
        void adminNoStatus() {
            Order order = order(user(Role.BUYER), OrderStatus.PAID);
            OrderResponse response = responseOf(order);
            when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(order), pageable, 1));
            when(orderMapper.toResponse(order)).thenReturn(response);

            Page<OrderResponse> result = orderService.getAllOrders(null, pageable, user(Role.ADMIN));

            assertSingleMappedPage(result, response);
            verify(orderRepository).findAll(pageable);
            verifyNoMoreInteractions(orderRepository);
        }

        @Test
        @DisplayName("ADMIN with status filter lists every order in that status")
        void adminWithStatus() {
            Order order = order(user(Role.BUYER), OrderStatus.PAID);
            OrderResponse response = responseOf(order);
            when(orderRepository.findByStatus(OrderStatus.PAID, pageable))
                    .thenReturn(new PageImpl<>(List.of(order), pageable, 1));
            when(orderMapper.toResponse(order)).thenReturn(response);

            Page<OrderResponse> result = orderService.getAllOrders(OrderStatus.PAID, pageable, user(Role.ADMIN));

            assertSingleMappedPage(result, response);
            verify(orderRepository).findByStatus(OrderStatus.PAID, pageable);
            verifyNoMoreInteractions(orderRepository);
        }

        @Test
        @DisplayName("BUYER without status filter only lists their own orders")
        void buyerNoStatus() {
            User buyer = user(Role.BUYER);
            Order order = order(buyer, OrderStatus.PENDING_PAYMENT);
            OrderResponse response = responseOf(order);
            when(orderRepository.findByUser(buyer, pageable)).thenReturn(new PageImpl<>(List.of(order), pageable, 1));
            when(orderMapper.toResponse(order)).thenReturn(response);

            Page<OrderResponse> result = orderService.getAllOrders(null, pageable, buyer);

            assertSingleMappedPage(result, response);
            verify(orderRepository).findByUser(buyer, pageable);
            verifyNoMoreInteractions(orderRepository);
        }

        @Test
        @DisplayName("BUYER with status filter only lists their own orders in that status")
        void buyerWithStatus() {
            User buyer = user(Role.BUYER);
            Order order = order(buyer, OrderStatus.SHIPPED);
            OrderResponse response = responseOf(order);
            when(orderRepository.findByUserAndStatus(buyer, OrderStatus.SHIPPED, pageable))
                    .thenReturn(new PageImpl<>(List.of(order), pageable, 1));
            when(orderMapper.toResponse(order)).thenReturn(response);

            Page<OrderResponse> result = orderService.getAllOrders(OrderStatus.SHIPPED, pageable, buyer);

            assertSingleMappedPage(result, response);
            verify(orderRepository).findByUserAndStatus(buyer, OrderStatus.SHIPPED, pageable);
            verifyNoMoreInteractions(orderRepository);
        }

        @Test
        @DisplayName("empty page is returned as an empty page")
        void emptyPage() {
            when(orderRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

            Page<OrderResponse> result = orderService.getAllOrders(null, pageable, user(Role.ADMIN));

            assertTrue(result.isEmpty());
            verifyNoInteractions(orderMapper);
        }
    }

    // -------------------------------------------------------- updateOrderStatus

    @Nested
    @DisplayName("updateOrderStatus(orderId, newStatus, requester)")
    class UpdateOrderStatus {

        @Test
        @DisplayName("404 – unknown order")
        void orderNotFound() {
            UUID orderId = UUID.randomUUID();
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertArtelier(HttpStatus.NOT_FOUND, "Order not found",
                    () -> orderService.updateOrderStatus(orderId, OrderStatus.SHIPPED, user(Role.ADMIN)));

            verifyNoInteractions(orderMapper);
        }

        @Test
        @DisplayName("ADMIN ships a PAID order")
        void adminShipsPaidOrder() {
            Order order = order(user(Role.BUYER), OrderStatus.PAID);
            OrderResponse response = responseOf(order);
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
            when(orderMapper.toResponse(order)).thenReturn(response);

            OrderResponse result = orderService.updateOrderStatus(order.getId(), OrderStatus.SHIPPED, user(Role.ADMIN));

            assertSame(response, result);
            assertEquals(OrderStatus.SHIPPED, order.getStatus());
        }

        @Test
        @DisplayName("400 – ADMIN can only move orders to SHIPPED")
        void adminOnlyShipped() {
            Order order = order(user(Role.BUYER), OrderStatus.PAID);
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

            assertArtelier(HttpStatus.BAD_REQUEST, "Admin can only transition orders to SHIPPED",
                    () -> orderService.updateOrderStatus(order.getId(), OrderStatus.CANCELLED, user(Role.ADMIN)));

            assertEquals(OrderStatus.PAID, order.getStatus());
            verifyNoInteractions(orderMapper);
        }

        @Test
        @DisplayName("400 – ADMIN cannot ship an order that is not PAID")
        void adminShipsNonPaidOrder() {
            Order order = order(user(Role.BUYER), OrderStatus.PENDING_PAYMENT);
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

            assertArtelier(HttpStatus.BAD_REQUEST,
                    "Order must be in PAID status to be shipped, current status: PENDING_PAYMENT",
                    () -> orderService.updateOrderStatus(order.getId(), OrderStatus.SHIPPED, user(Role.ADMIN)));

            assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
            verifyNoInteractions(orderMapper);
        }

        @Test
        @DisplayName("BUYER cancels their own order while pending payment")
        void buyerCancelsOwnOrder() {
            User buyer = user(Role.BUYER);
            Order order = order(buyer, OrderStatus.PENDING_PAYMENT);
            OrderResponse response = responseOf(order);
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
            when(orderMapper.toResponse(order)).thenReturn(response);

            OrderResponse result = orderService.updateOrderStatus(order.getId(), OrderStatus.CANCELLED, buyer);

            assertSame(response, result);
            assertEquals(OrderStatus.CANCELLED, order.getStatus());
        }

        @Test
        @DisplayName("400 – BUYER can only cancel, never set another status")
        void buyerOnlyCancel() {
            User buyer = user(Role.BUYER);
            Order order = order(buyer, OrderStatus.PENDING_PAYMENT);
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

            assertArtelier(HttpStatus.BAD_REQUEST, "You can only cancel your order",
                    () -> orderService.updateOrderStatus(order.getId(), OrderStatus.PAID, buyer));

            assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
            verifyNoInteractions(orderMapper);
        }

        @Test
        @DisplayName("400 – BUYER cannot cancel an order that is no longer pending payment")
        void buyerCancelsNonPendingOrder() {
            User buyer = user(Role.BUYER);
            Order order = order(buyer, OrderStatus.PAID);
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

            assertArtelier(HttpStatus.BAD_REQUEST,
                    "Order can only be cancelled when pending payment, current status: PAID",
                    () -> orderService.updateOrderStatus(order.getId(), OrderStatus.CANCELLED, buyer));

            assertEquals(OrderStatus.PAID, order.getStatus());
            verifyNoInteractions(orderMapper);
        }

        @Test
        @DisplayName("403 – BUYER cannot cancel someone else's pending order")
        void buyerCancelsForeignOrder() {
            Order order = order(user(Role.BUYER), OrderStatus.PENDING_PAYMENT);
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

            assertArtelier(HttpStatus.FORBIDDEN, "Access denied to this order",
                    () -> orderService.updateOrderStatus(order.getId(), OrderStatus.CANCELLED, user(Role.BUYER)));

            assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
            verifyNoInteractions(orderMapper);
        }
    }

    // ----------------------------------------------- updateOrderStatusInternal

    @Nested
    @DisplayName("updateOrderStatusInternal(orderId, newStatus)")
    class UpdateOrderStatusInternal {

        @Test
        @DisplayName("404 – unknown order")
        void orderNotFound() {
            UUID orderId = UUID.randomUUID();
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertArtelier(HttpStatus.NOT_FOUND, "Order not found",
                    () -> orderService.updateOrderStatusInternal(orderId, OrderStatus.PAID));

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("400 – a cancelled order cannot be modified")
        void cancelledOrderIsLocked() {
            Order order = order(user(Role.BUYER), OrderStatus.CANCELLED);
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

            assertArtelier(HttpStatus.BAD_REQUEST, "Cannot modify a cancelled order",
                    () -> orderService.updateOrderStatusInternal(order.getId(), OrderStatus.PAID));

            assertEquals(OrderStatus.CANCELLED, order.getStatus());
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("updates the status and saves the order")
        void updatesAndSaves() {
            Order order = order(user(Role.BUYER), OrderStatus.PENDING_PAYMENT);
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

            orderService.updateOrderStatusInternal(order.getId(), OrderStatus.PAID);

            assertEquals(OrderStatus.PAID, order.getStatus());
            verify(orderRepository).save(order);
        }
    }

    // ------------------------------------------------------------ getOrderById

    @Nested
    @DisplayName("getOrderById(orderId, requester)")
    class GetOrderById {

        @Test
        @DisplayName("404 – unknown order")
        void orderNotFound() {
            UUID orderId = UUID.randomUUID();
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertArtelier(HttpStatus.NOT_FOUND, "Order not found",
                    () -> orderService.getOrderById(orderId, user(Role.ADMIN)));

            verifyNoInteractions(orderMapper);
        }

        @Test
        @DisplayName("owner can read their own order")
        void ownerCanRead() {
            User buyer = user(Role.BUYER);
            Order order = order(buyer, OrderStatus.PAID);
            OrderResponse response = responseOf(order);
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
            when(orderMapper.toResponse(order)).thenReturn(response);

            assertSame(response, orderService.getOrderById(order.getId(), buyer));
        }

        @Test
        @DisplayName("ADMIN can read any order")
        void adminCanReadAny() {
            Order order = order(user(Role.BUYER), OrderStatus.PAID);
            OrderResponse response = responseOf(order);
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
            when(orderMapper.toResponse(order)).thenReturn(response);

            assertSame(response, orderService.getOrderById(order.getId(), user(Role.ADMIN)));
        }

        @Test
        @DisplayName("403 – another BUYER cannot read the order and it is never mapped")
        void otherBuyerDenied() {
            Order order = order(user(Role.BUYER), OrderStatus.PAID);
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

            assertArtelier(HttpStatus.FORBIDDEN, "Access denied to this order",
                    () -> orderService.getOrderById(order.getId(), user(Role.BUYER)));

            verifyNoInteractions(orderMapper);
        }
    }
}