
package com.artelier.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
        name = "ProductImageResponse",
        description = "Represents an image associated with a product",
        example = """
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "url": "https://res.cloudinary.com/artelier/image/upload/products/mug.jpg",
      "cloudinaryId": "products/mug_abc123",
      "isPrimary": true,
      "sortOrder": 0
    }
    """
)
public record ProductImageResponse(

        @Schema(
                description = "Unique identifier of the product image",
                example = "550e8400-e29b-41d4-a716-446655440001",
                format = "uuid",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Public URL of the image",
                example = "https://res.cloudinary.com/artelier/image/upload/products/mug.jpg",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String url,

        @Schema(
                description = "Cloudinary public ID used to manage the image",
                example = "products/mug_abc123",
                nullable = true
        )
        String cloudinaryId,

        @Schema(
                description = "Indicates whether this is the primary product image",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("isPrimary")
        Boolean isPrimary,

        @Schema(
                description = "Display order of the image",
                example = "0",
                minimum = "0",
                nullable = true
        )
        Integer sortOrder

) {}
