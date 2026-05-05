package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Request payload for creating a new order")
public class OrderRequest {

    @NotEmpty
    @Schema(
            description = "List of items included in the order",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<OrderItemRequest> items;

    @NotBlank
    @Schema(
            description = "Shipping address for the order",
            example = "Cra 123 #45-67, Bogotá, Colombia",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String shippingAddress;

    @Schema(
            description = "Optional notes for the order",
            example = "Please deliver after 5 PM"
    )
    private String notes;

    @Data
    @Schema(description = "Single item inside an order")
    public static class OrderItemRequest {

        @NotNull
        @Schema(
                description = "Product UUID",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        private UUID productId;

        @Min(1)
        @Schema(
                description = "Quantity of the product",
                example = "2"
        )
        private int quantity;

        @Schema(
                description = "Optional customization notes for this item",
                example = "No onions, extra cheese"
        )
        private String customNotes;
    }
}