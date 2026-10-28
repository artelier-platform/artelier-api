package com.artelier.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PositionRequest(

        @NotNull(message = "newOrder is required")
        @Min(value = 1, message = "newOrder must be at least 1")
        Integer newOrder

) {
}
