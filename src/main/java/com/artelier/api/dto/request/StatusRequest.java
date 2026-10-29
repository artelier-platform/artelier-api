package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload to activate or deactivate a resource.
 *
 * @param isActive {@code true} to activate the resource, {@code false} to deactivate it
 */
@Schema(description = "Request payload to activate or deactivate a resource")
public record StatusRequest(

        @Schema(
                description = "true to activate the resource, false to deactivate it",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull
        Boolean isActive

) {}
