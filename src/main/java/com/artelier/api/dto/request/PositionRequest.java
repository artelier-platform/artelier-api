package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload to move an element to a new position inside an ordered list.
 *
 * @param newOrder new position of the element, starting at 1
 */
@Schema(description = "Request payload to move an element to a new position inside an ordered list")
public record PositionRequest(

        @Schema(
                description = "New position of the element, starting at 1",
                example = "2",
                minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "newOrder is required")
        @Min(value = 1, message = "newOrder must be at least 1")
        Integer newOrder

) {}
