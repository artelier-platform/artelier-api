
package com.artelier.api.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;
import com.artelier.api.catalog.entity.Product;

@Schema(description = "Represents a single item in an order response")
public record OrderItemResponse(

        @Schema(
                description = "Product UUID",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID productId,

        @Schema(
                description = "Product name",
                example = "Handmade Ceramic Mug"
        )
        String productName,

        @Schema(
                description = "Quantity ordered",
                example = "2"
        )
        int quantity,

        @Schema(
                description = "Unit price at the time of purchase",
                example = "29.99"
        )
        BigDecimal unitPrice,

        @Schema(
                description = "Optional notes for this item",
                example = "Gift wrap this item"
        )
        String customNotes

) {
}
