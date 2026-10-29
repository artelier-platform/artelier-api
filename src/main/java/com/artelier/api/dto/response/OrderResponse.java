
package com.artelier.api.dto.response;

import com.artelier.api.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
@Schema(
        name = "OrderResponse",
        description = "Represents an order response"
)
public record OrderResponse(

        @Schema(
                description = "Order UUID",
                example = "550e8400-e29b-41d4-a716-446655440000",
                format = "uuid",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Current status of the order",
                example = "PENDING_PAYMENT",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        OrderStatus status,

        @Schema(
                description = "Subtotal amount",
                example = "59.98",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        BigDecimal subtotal,

        @Schema(
                description = "Total amount including taxes and shipping",
                example = "59.98",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        BigDecimal total,

        @Schema(
                description = "Shipping address",
                example = "Cra 123 #45-67, Bogotá, Colombia",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String shippingAddress,

        @Schema(
                description = "Optional notes",
                example = "Leave at the door",
                nullable = true
        )
        String notes,

        @Schema(
                description = "Order creation timestamp",
                example = "2026-05-04T15:30:00Z",
                format = "date-time",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant createdAt,

        @Schema(
                description = "List of items in the order",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        List<OrderItemResponse> items

) {}
