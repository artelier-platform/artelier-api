package com.artelier.api.service.impl;

import com.artelier.api.dto.projection.TopProductProjection;
import com.artelier.api.dto.response.StatsResponse;
import com.artelier.api.enums.OrderStatus;
import com.artelier.api.repository.OrderItemRepository;
import com.artelier.api.repository.OrderRepository;
import com.artelier.api.repository.ProductRepository;
import com.artelier.api.service.StatsService;
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
                        top.get(0).getId(),
                        top.get(0).getName(),
                        top.get(0).getTotalSold()
                );

        return new StatsResponse(totalSales, pendingOrders, activeProducts, topProduct);
    }
}