package com.artelier.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record StatsResponse(
        BigDecimal totalSalesThisMonth,
        long pendingOrders,
        long activeProducts,
        TopProduct topSellingProduct
) {
    public record TopProduct(
            UUID id,
            String name,
            long totalSold
    ) {}
}