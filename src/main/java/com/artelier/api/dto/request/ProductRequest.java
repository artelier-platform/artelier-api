package com.artelier.api.dto.request;

import com.artelier.api.enums.StockType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Product creation/update request")
public class ProductRequest {

    @Schema(
            description = "Category UUID",
            example = "550e8400-e29b-41d4-a716-446655440000"
    )
    @NotNull(message = "Category is required")
    private UUID categoryId;

    @Schema(example = "Handmade Ceramic Mug")
    @NotBlank(message = "Name is required")
    @Size(max = 150)
    private String name;

    @Schema(example = "Beautiful handmade mug")
    @Size(max = 500)
    private String description;

    @Schema(example = "Crafted by local artisans...")
    private String story;

    @Schema(example = "49.99")
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @Schema(example = "AVAILABLE")
    @NotNull
    private StockType stockType;

    @Schema(example = "10")
    @Min(0)
    private Integer stockQuantity;

    @Schema(example = "false")
    @NotNull
    private Boolean isCustomOrder;

    @Schema(example = "true")
    @NotNull
    private Boolean isActive;

    @Valid
    private List<ImageMetadata> images;
}