package com.artelier.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BannerRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 300) String description,
        @Size(max = 40) String buttonLabel,
        @Size(max = 500) String buttonUrl,
        @NotNull @Min(0) Integer sortOrder,
        Boolean isActive
) {}
