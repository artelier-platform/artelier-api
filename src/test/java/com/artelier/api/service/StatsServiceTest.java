package com.artelier.api.service;

import com.artelier.api.dto.projection.TopProductProjection;
import com.artelier.api.dto.response.StatsResponse;
import com.artelier.api.enums.OrderStatus;
import com.artelier.api.repository.OrderItemRepository;
import com.artelier.api.repository.OrderRepository;
import com.artelier.api.repository.ProductRepository;
import com.artelier.api.service.impl.StatsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private StatsServiceImpl statsService;


    @Test
    void shouldReturnStatsSuccessfully() {

        UUID productId = UUID.randomUUID();

        TopProductProjection projection = new TopProductProjection() {
            @Override
            public UUID getId() {
                return productId;
            }

            @Override
            public String getName() {
                return "Gojo Figure";
            }

            @Override
            public Long getTotalSold() {
                return 40L;
            }
        };

        when(orderRepository.sumTotalSalesSince(any(Instant.class)))
                .thenReturn(BigDecimal.valueOf(5000));

        when(orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT))
                .thenReturn(7L);

        when(productRepository.countByIsActiveTrue())
                .thenReturn(18L);

        when(orderItemRepository.findTopSellingProducts(any(Pageable.class)))
                .thenReturn(List.of(projection));

        StatsResponse result = statsService.getStats();

        assertNotNull(result);

        assertEquals(BigDecimal.valueOf(5000), result.totalSalesThisMonth());
        assertEquals(7L, result.pendingOrders());
        assertEquals(18L, result.activeProducts());

        assertNotNull(result.topSellingProduct());
        assertEquals(productId, result.topSellingProduct().id());
        assertEquals("Gojo Figure", result.topSellingProduct().name());
        assertEquals(40L, result.topSellingProduct().totalSold());
    }


    @Test
    void shouldReturnStatsWithoutTopProduct() {

        when(orderRepository.sumTotalSalesSince(any(Instant.class)))
                .thenReturn(BigDecimal.ZERO);

        when(orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT))
                .thenReturn(0L);

        when(productRepository.countByIsActiveTrue())
                .thenReturn(0L);

        when(orderItemRepository.findTopSellingProducts(any(Pageable.class)))
                .thenReturn(List.of());

        StatsResponse result = statsService.getStats();

        assertNotNull(result);

        assertEquals(BigDecimal.ZERO, result.totalSalesThisMonth());
        assertEquals(0L, result.pendingOrders());
        assertEquals(0L, result.activeProducts());

        assertNull(result.topSellingProduct());
    }
}