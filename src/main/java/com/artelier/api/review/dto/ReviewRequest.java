package com.artelier.api.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;
import com.artelier.api.catalog.entity.Product;

/**
 * Request used to create a review for a purchased product.
 *
 * <p>The order item identifies the product being reviewed.
 * The authenticated user is automatically associated with the review
 * by the server.</p>
 *
 * <p>The rating must be between 1 and 5.
 * The comment is optional and cannot exceed 2000 characters.</p>
 *
 * <p>The server must verify that the order item belongs to the
 * authenticated user, that the purchase satisfies the review
 * eligibility requirements, and that the item has not already
 * been reviewed.</p>
 *
 * @param orderItemId identifier of the purchased order item
 * @param rating      product rating (1 to 5)
 * @param comment     optional review comment (maximum 2000 characters)
 */
@Schema(description = "Request used to create a product review")
public record ReviewRequest(

        @Schema(
                description = "Identifier of the purchased order item being reviewed",
                example = "550e8400-e29b-41d4-a716-446655440000",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull
        UUID orderItemId,

        @Schema(
                description = "Product rating from 1 to 5",
                example = "5",
                minimum = "1",
                maximum = "5",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull
        @Min(1)
        @Max(5)
        Integer rating,

        @Schema(
                description = "Optional review comment",
                example = "Excellent product and great quality.",
                maxLength = 2000
        )
        @Size(max = 2000)
        String comment

) { }