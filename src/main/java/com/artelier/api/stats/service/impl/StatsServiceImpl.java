package com.artelier.api.stats.service.impl;

import com.artelier.api.order.dto.TopProductProjection;
import com.artelier.api.stats.dto.StatsResponse;
import com.artelier.api.order.enums.OrderStatus;
import com.artelier.api.order.repository.OrderItemRepository;
import com.artelier.api.order.repository.OrderRepository;
import com.artelier.api.catalog.repository.ProductRepository;
import com.artelier.api.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public StatsResponse getStats() {

        Instant startOfMonth = YearMonth.now(ZoneOffset.UTC)
                .atDay(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        BigDecimal totalSales = orderRepository.sumTotalSalesSince(startOfMonth);

        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT);

        long activeProducts = productRepository.countByIsActiveTrue();

        List<TopProductProjection> top = orderItemRepository
                .findTopSellingProducts(PageRequest.of(0, 1));

        StatsResponse.TopProduct topProduct = top.isEmpty() ? null :
                new StatsResponse.TopProduct(
                        top.getFirst().getId(),
                        top.getFirst().getName(),
                        top.getFirst().getTotalSold()
                );

        return new StatsResponse(totalSales, pendingOrders, activeProducts, topProduct);
    }
}