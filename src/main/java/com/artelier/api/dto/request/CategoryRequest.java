package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload to create a category.
 *
 * @param name        display name of the category (required, up to 100 characters)
 * @param slug        URL-friendly unique identifier (required, up to 120 characters)
 * @param description optional description (up to 500 characters)
 */
@Schema(description = "Category creation request")
public record CategoryRequest(

        @Schema(
                description = "Display name of the category",
                example = "Ceramics",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Name is required")
        @Size(max = 100)
        String name,

        @Schema(
                description = "URL-friendly unique identifier",
                example = "ceramics",
                maxLength = 120,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Slug is required")
        @Size(max = 120)
        String slug,

        @Schema(
                description = "Optional category description",
                example = "Handmade ceramic products including mugs and bowls",
                maxLength = 500
        )
        @Size(max = 500)
        String description

) {}
