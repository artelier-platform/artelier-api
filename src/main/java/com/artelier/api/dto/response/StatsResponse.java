
package com.artelier.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(
        name = "StatsResponse",
        description = "Aggregated metrics displayed in the administrative dashboard"
)
public record StatsResponse(

        @Schema(
                description = "Sum of paid sales generated during the current UTC month",
                example = "12450.75",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        BigDecimal totalSalesThisMonth,

        @Schema(
                description = "Number of orders waiting for payment",
                example = "8",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        long pendingOrders,

        @Schema(
                description = "Number of products currently visible in the catalog",
                example = "42",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        long activeProducts,

        @Schema(
                description = "Best-selling product, or null when no product has been sold",
                nullable = true
        )
        TopProduct topSellingProduct

) {

    @Schema(
            name = "TopProduct",
            description = "Best-selling product summary"
    )
    public record TopProduct(

            @Schema(
                    description = "Unique identifier of the product",
                    example = "550e8400-e29b-41d4-a716-446655440000",
                    format = "uuid",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            UUID id,

            @Schema(
                    description = "Product display name",
                    example = "Handmade Ceramic Mug",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String name,

            @Schema(
                    description = "Accumulated quantity sold",
                    example = "125",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            long totalSold

    ) {}
}
