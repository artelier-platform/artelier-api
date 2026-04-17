package com.artelier.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(
        name = "ProductImageResponse",
        description = "Represents an image associated with a product returned by the API",
        example = """
        {
          "id": "550e8400-e29b-41d4-a716-446655440001",
          "url": "https://res.cloudinary.com/artelier/image/upload/v1712345678/products/mug.jpg",
          "cloudinaryId": "products/mug_abc123",
          "isPrimary": true,
          "sortOrder": 0
        }
        """
)
public class ProductImageResponse {

    @Schema(
            description = "Unique identifier of the product image",
            example = "550e8400-e29b-41d4-a716-446655440001",
            format = "uuid"
    )
    private UUID id;

    @Schema(
            description = "Public URL of the image",
            example = "https://res.cloudinary.com/artelier/image/upload/v1712345678/products/mug.jpg"
    )
    private String url;

    @Schema(
            description = "Cloudinary public ID used to manage the image",
            example = "products/mug_abc123",
            nullable = true
    )
    private String cloudinaryId;

    @Schema(
            description = "Indicates whether this image is the primary product image",
            example = "true"
    )
    private Boolean isPrimary;

    @Schema(
            description = "Display order of the image (lower values appear first)",
            example = "0",
            minimum = "0",
            nullable = true
    )
    private Integer sortOrder;
}