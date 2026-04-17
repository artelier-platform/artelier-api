package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(
        name = "ProductImageRequest",
        description = "Represents an image associated with a product"
)
public class ProductImageRequest {

    @Schema(
            description = "Public URL of the product image",
            example = "https://res.cloudinary.com/artelier/image/upload/v1712345678/products/mug.jpg",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "URL is required")
    private String url;

    @Schema(
            description = "Cloudinary public ID used to manage the uploaded image",
            example = "products/mug_abc123",
            nullable = true
    )
    private String cloudinaryId;

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