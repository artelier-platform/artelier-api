package com.artelier.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record StatusRequest(
        @NotNull Boolean isActive
) {}