package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Category creation request")
public class CategoryRequest {

    @Schema(
            description = "Display name of the category",
            example = "Ceramics",
            maxLength = 100
    )
    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @Schema(
            description = "URL-friendly unique identifier",
            example = "ceramics",
            maxLength = 120
    )
    @NotBlank(message = "Slug is required")
    @Size(max = 120)
    private String slug;

    @Schema(
            description = "Optional category description",
            example = "Handmade ceramic products including mugs and bowls"
    )
    @Size(max = 500)
    private String description;
}