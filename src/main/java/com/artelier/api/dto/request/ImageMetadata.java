package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(
        name = "ProductImageRequest",
        description = "Represents an image associated with a product"
)
public class ImageMetadata {
    @Schema(
            description = "Indicates whether this image is the main (primary) product image",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    private Boolean isPrimary;

    @Schema(
            description = "Display order of the image (lower values appear first)",
            example = "0",
            minimum = "0",
            nullable = true
    )
    @Min(0)
    private Integer sortOrder;
}