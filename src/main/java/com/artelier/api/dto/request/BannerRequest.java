package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request payload that defines a banner and the items that compose it.
 *
 * @param isActive optional activation flag of the banner
 * @param items    items of the banner; between 1 and 20, none of them {@code null}
 */
@Schema(description = "Request payload that defines a banner and the items that compose it")
public record BannerRequest(

        @Schema(
                description = "Whether the banner is active. Optional.",
                example = "true",
                nullable = true
        )
        Boolean isActive,

        @Schema(
                description = "Items that compose the banner. Between 1 and 20 items are allowed.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull
        @Size(min = 1, max = 20)
        List<@NotNull @Valid ItemBannerRequest> items

) {}
