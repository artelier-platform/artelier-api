package com.artelier.api.dto.response;

import com.artelier.api.enums.StockType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Schema(
        name = "ProductResponse",
        description = "Represents a product returned by the API, including category and images",
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
          "story": "Crafted by local artisans using traditional techniques",
          "price": 49.99,
          "stockType": "FINITE",
          "stockQuantity": 10,
          "isCustomOrder": false,
          "isActive": true,
          "createdAt": "2026-01-15T10:30:00Z",
          "images": [
            {
              "id": "550e8400-e29b-41d4-a716-446655440001",
              "url": "https://res.cloudinary.com/artelier/image/upload/v1712345678/products/mug.jpg",
              "cloudinaryId": "products/mug_abc123",
              "isPrimary": true,
              "sortOrder": 0
            }
          ]
        }
        """
)
public class ProductResponse {

    @Schema(
            description = "Unique identifier of the product",
            example = "550e8400-e29b-41d4-a716-446655440000",
            format = "uuid"
    )
    private UUID id;

    @Schema(
            description = "Category to which the product belongs"
    )
    private CategoryResponse category;

    @Schema(
            description = "Product display name",
            example = "Handmade Ceramic Mug",
            maxLength = 150
    )
    private String name;

    @Schema(
            description = "SEO-friendly slug used in URLs",
            example = "handmade-ceramic-mug",
            maxLength = 160
    )
    private String slug;

    @Schema(
            description = "Short product description",
            example = "Beautiful handmade ceramic mug",
            nullable = true,
            maxLength = 500
    )
    private String description;

    @Schema(
            description = "Optional story or background about the product",
            example = "Crafted by local artisans using traditional techniques",
            nullable = true
    )
    private String story;

    @Schema(
            description = "Product price",
            example = "49.99",
            minimum = "0"
    )
    private BigDecimal price;

    @Schema(
            description = "Defines how stock is handled",
            implementation = StockType.class
    )
    private StockType stockType;

    @Schema(
            description = "Available stock quantity. Relevant only for AVAILABLE and MADE_TO_ORDER stock types",
            example = "10",
            minimum = "0",
            nullable = true
    )
    private Integer stockQuantity;

    @Schema(
            description = "Indicates whether the product supports custom orders",
            example = "false"
    )
    private Boolean isCustomOrder;

    @Schema(
            description = "Indicates whether the product is active and visible",
            example = "true"
    )
    private Boolean isActive;

    @Schema(
            description = "Timestamp when the product was created",
            example = "2026-01-15T10:30:00Z",
            format = "date-time"
    )
    private Instant createdAt;

    @ArraySchema(
            schema = @Schema(implementation = ProductImageResponse.class),
            arraySchema = @Schema(
                    description = "List of images associated with the product"
            )
    )
    private List<ProductImageResponse> images;
}