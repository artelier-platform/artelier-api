
package com.artelier.api.dto.response;

import com.artelier.api.enums.StockType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(
        name = "ProductResponse",
        description = "Product response including category and images",
        example = """
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "category": {
        "id": "550e8400-e29b-41d4-a716-446655440111",
        "name": "Ceramics",
        "slug": "ceramics",
        "description": "Handmade ceramic products"
      },
      "name": "Handmade Ceramic Mug",
      "slug": "handmade-ceramic-mug",
      "description": "Beautiful handmade ceramic mug",
      "story": "Crafted by local artisans",
      "price": 49.99,
      "stockType": "AVAILABLE",
      "stockQuantity": 10,
      "isCustomOrder": false,
      "isActive": true,
      "createdAt": "2026-01-15T10:30:00Z",
      "images": []
    }
    """
)
public record ProductResponse(

        @Schema(
                description = "Unique identifier of the product",
                example = "550e8400-e29b-41d4-a716-446655440000",
                format = "uuid",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Product category",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        CategoryResponse category,

        @Schema(
                description = "Product display name",
                example = "Handmade Ceramic Mug",
                maxLength = 150,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String name,

        @Schema(
                description = "SEO-friendly slug used in URLs",
                example = "handmade-ceramic-mug",
                maxLength = 160,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String slug,

        @Schema(
                description = "Short product description",
                example = "Beautiful handmade ceramic mug",
                nullable = true,
                maxLength = 500
        )
        String description,

        @Schema(
                description = "Optional story or background about the product",
                example = "Crafted by local artisans",
                nullable = true
        )
        String story,

        @Schema(
                description = "Product price",
                example = "49.99",
                minimum = "0",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        BigDecimal price,

        @Schema(
                description = "Defines how stock is handled",
                implementation = StockType.class,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        StockType stockType,

        @Schema(
                description = "Available stock quantity; relevant for AVAILABLE and MADE_TO_ORDER",
                example = "10",
                minimum = "0",
                nullable = true
        )
        Integer stockQuantity,

        @Schema(
                description = "Whether the product supports custom orders",
                example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("isCustomOrder")
        Boolean isCustomOrder,

        @Schema(
                description = "Whether the product is active and visible",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("isActive")
        Boolean isActive,

        @Schema(
                description = "Timestamp when the product was created",
                example = "2026-01-15T10:30:00Z",
                format = "date-time",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant createdAt,

        @ArraySchema(
                schema = @Schema(implementation = ProductImageResponse.class),
                arraySchema = @Schema(
                        description = "Images associated with the product",
                        requiredMode = Schema.RequiredMode.REQUIRED
                )
        )
        List<ProductImageResponse> images

) {}
