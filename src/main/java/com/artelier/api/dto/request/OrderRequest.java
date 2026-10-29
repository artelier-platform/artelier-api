package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for creating a new order.
 *
 * @param items           items included in the order; at least one is required
 * @param shippingAddress shipping address of the order (required)
 * @param notes           optional notes for the whole order
 */
@Schema(description = "Request payload for creating a new order")
public record OrderRequest(

        @Schema(
                description = "List of items included in the order. At least one item is required.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotEmpty
        @Valid
        List<OrderItemRequest> items,

        @Schema(
                description = "Shipping address for the order",
                example = "Cra 123 #45-67, Bogotá, Colombia",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        String shippingAddress,

        @Schema(
                description = "Optional notes for the order",
                example = "Please deliver after 5 PM"
        )
        String notes

) {

    /**
     * Single item inside an order.
     *
     * @param productId   identifier of the product (required)
     * @param quantity    number of units; at least 1
     * @param customNotes optional customization notes for this item
     */
    @Schema(description = "Single item inside an order")
    public record OrderItemRequest(

            @Schema(
                    description = "Product UUID",
                    example = "550e8400-e29b-41d4-a716-446655440000",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            @NotNull
            UUID productId,

            @Schema(
                    description = "Quantity of the product. Must be at least 1.",
                    example = "2",
                    minimum = "1",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            @Min(1)
            int quantity,

            @Schema(
                    description = "Optional customization notes for this item",
                    example = "Gift wrap this one"
            )
            String customNotes

    ) {}
}
