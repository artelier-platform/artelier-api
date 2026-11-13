package com.artelier.api.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;
import com.artelier.api.catalog.entity.Product;

/**
 * Response containing a product review.
 *
 * <p>Includes the review identifier, author information, product identifier,
 * rating, optional comment, and creation and modification timestamps.</p>
 *
 * <p>The author and product information are derived from their
 * corresponding entities rather than supplied by the client.</p>
 *
 * @param id        unique review identifier
 * @param userId    identifier of the user who created the review
 * @param username username of the review author
 * @param productId identifier of the reviewed product
 * @param rating   product rating (between 1 and 5)
 * @param comment  optional review comment
 * @param createdAt timestamp when the review was created
 * @param updatedAt timestamp when the review was last modified
 */
@Schema(
        name = "ReviewResponse",
        description = "Product review information"
)
public record ReviewResponse(

        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(example = "550e8400-e29b-41d4-a716-446655440001")
        UUID userId,

        @Schema(example = "Geronimo01")
        String username,

        @Schema(example = "550e8400-e29b-41d4-a716-446655440002")
        UUID productId,

        @Schema(
                description = "Product rating",
                example = "5",
                minimum = "1",
                maximum = "5"
        )
        int rating,

        @Schema(
                description = "Optional review comment",
                example = "Excellent product and great quality.",
                maxLength = 2000
        )
        String comment,

        @Schema(example = "2026-11-05T18:00:00Z")
        Instant createdAt,

        @Schema(example = "2026-11-05T19:00:00Z")
        Instant updatedAt

) {
}