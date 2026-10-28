package com.artelier.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BannerResponse(
        UUID id,
        boolean isActive,
        Instant createdAt,
        List<ItemBannerResponse> items
) {}