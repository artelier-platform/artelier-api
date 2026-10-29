package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Metadata of an image associated with a product.
 *
 * <p>Exposed in the OpenAPI document as {@code ProductImageRequest}.</p>
 *
 * @param isPrimary whether this is the main (primary) product image
 * @param sortOrder optional display order; lower values appear first
 */
@Schema(
        name = "ProductImageRequest",
        description = "Metadata of an image associated with a product"
)
public record ImageMetadata(

        @Schema(
                description = "Indicates whether this image is the main (primary) product image",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull
        Boolean isPrimary,

        @Schema(
                description = "Display order of the image (lower values appear first)",
                example = "0",
                minimum = "0",
                nullable = true
        )
        @Min(0)
        Integer sortOrder

) {}
