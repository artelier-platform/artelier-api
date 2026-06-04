package com.artelier.api.dto.response;

import com.artelier.api.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Represents an order response")
public class OrderResponse {

    @Schema(
            description = "Order UUID",
            example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private UUID id;

    @Schema(
            description = "Current status of the order",
            example = "PENDING_PAYMENT"
    )
    private OrderStatus status;

    @Schema(
            description = "Subtotal amount",
            example = "59.98"
    )
    private BigDecimal subtotal;

    @Schema(
            description = "Total amount (including taxes, shipping, etc.)",
            example = "59.98"
    )
    private BigDecimal total;

    @Schema(
            description = "Shipping address",
            example = "Cra 123 #45-67, Bogotá, Colombia"
    )
    private String shippingAddress;

    @Schema(
            description = "Optional notes",
            example = "Leave at the door"
    )
    private String notes;

    @Schema(
            description = "Order creation timestamp",
            example = "2026-05-04T15:30:00Z"
    )
    private Instant createdAt;

    @Schema(
            description = "List of items in the order"
    )
    private List<OrderItemResponse> items;
}