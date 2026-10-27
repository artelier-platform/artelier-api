package com.artelier.api.dto.response;

import com.artelier.api.entity.Banner;

import java.util.UUID;

public record BannerResponse(
        UUID id,
        String title,
        String description,
        String imageUrl,
        String buttonLabel,
        String buttonUrl,
        int sortOrder,
        boolean isActive
) {
    public static BannerResponse from(Banner b) {
        return new BannerResponse(
                b.getId(), b.getTitle(), b.getDescription(), b.getImageUrl(),
                b.getButtonLabel(), b.getButtonUrl(),
                b.getSortOrder() == null ? 0 : b.getSortOrder(),
                Boolean.TRUE.equals(b.getIsActive()));
    }
}
