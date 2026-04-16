package com.artelier.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
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
public class CategoryResponse {

    @Schema(
            description = "Unique identifier of the category",
            example = "550e8400-e29b-41d4-a716-446655440000",
            format = "uuid"
    )
    private UUID id;

    @Schema(
            description = "Display name of the category",
            example = "Ceramics",
            maxLength = 100
    )
    private String name;

    @Schema(
            description = "URL-friendly identifier used in routes",
            example = "ceramics",
            maxLength = 120
    )
    private String slug;

    @Schema(
            description = "Optional description explaining the category",
            example = "Handmade ceramic products including mugs and bowls",
            nullable = true,
            maxLength = 500
    )
    private String description;
}