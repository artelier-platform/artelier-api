
package com.artelier.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A banner together with its items.
 *
 * @param id unique identifier of the banner
 * @param isActive whether the banner is active
 * @param createdAt creation timestamp
 * @param items items that compose the banner
 */
@Schema(
        name = "BannerResponse",
        description = "A banner together with its items"
)
public record BannerResponse(

        @Schema(
                description = "Unique identifier of the banner",
                example = "550e8400-e29b-41d4-a716-446655440000",
                format = "uuid",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Whether the banner is active",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean isActive,

        @Schema(
                description = "Timestamp when the banner was created",
                example = "2026-01-15T10:30:00Z",
                format = "date-time",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant createdAt,

        @Schema(
                description = "Items that compose the banner",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        List<ItemBannerResponse> items

) {}
