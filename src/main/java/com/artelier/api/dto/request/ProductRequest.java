package com.artelier.api.dto.request;

import com.artelier.api.enums.StockType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request payload to create or update a product.
 *
 * @param categoryId    identifier of the category the product belongs to (required)
 * @param name          product name (required, up to 150 characters)
 * @param description   short description (up to 500 characters)
 * @param story         free-form story behind the product
 * @param price         unit price; must be greater than zero
 * @param stockType     stock handling strategy
 * @param stockQuantity stock quantity; meaningful for {@code AVAILABLE} and {@code MADE_TO_ORDER}
 * @param isCustomOrder whether the product is a custom order
 * @param isActive      whether the product is visible in the catalog
 * @param images        metadata of the product images, coordinated by index with the uploaded files
 */
@Schema(description = "Product creation/update request")
public record ProductRequest(

        @Schema(
                description = "Category UUID",
                example = "550e8400-e29b-41d4-a716-446655440000",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Category is required")
        UUID categoryId,

        @Schema(
                description = "Product name. The slug is generated from it.",
                example = "Handmade Ceramic Mug",
                maxLength = 150,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Name is required")
        @Size(max = 150)
        String name,

        @Schema(
                description = "Short product description",
                example = "Beautiful handmade mug",
                maxLength = 500
        )
        @Size(max = 500)
        String description,

        @Schema(
                description = "Story behind the product",
                example = "Crafted by local artisans..."
        )
        String story,

        @Schema(
                description = "Unit price. Must be greater than zero.",
                example = "49.99",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal price,

        @Schema(
                description = "Stock handling strategy",
                example = "AVAILABLE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull
        StockType stockType,

        @Schema(
                description = "Stock quantity. Required for AVAILABLE and MADE_TO_ORDER stock types",
                example = "10",
                minimum = "0",
                nullable = true
        )
        @Min(0)
        Integer stockQuantity,

        @Schema(
                description = "Whether the product is a custom order",
                example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull
        Boolean isCustomOrder,

        @Schema(
                description = "Whether the product is visible in the catalog",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull
        Boolean isActive,

        @Schema(description = "Metadata of the product images, coordinated by index with the uploaded files")
        @Valid
        List<ImageMetadata> images

) {}
