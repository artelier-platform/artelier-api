
package com.artelier.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
        name = "CategoryResponse",
        description = "Represents a product category returned by the API",
        example = """
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Ceramics",
      "slug": "ceramics",
      "description": "Handmade ceramic products including mugs and bowls"
    }
    """
)
public record CategoryResponse(

        @Schema(
                description = "Unique identifier of the category",
                example = "550e8400-e29b-41d4-a716-446655440000",
                format = "uuid",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Display name of the category",
                example = "Ceramics",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String name,

        @Schema(
                description = "URL-friendly identifier used in routes",
                example = "ceramics",
                maxLength = 120,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String slug,

        @Schema(
                description = "Optional description explaining the category",
                example = "Handmade ceramic products including mugs and bowls",
                nullable = true,
                maxLength = 500
        )
        String description

) {}
