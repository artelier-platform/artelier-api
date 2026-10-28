package com.artelier.api.dto.response;

import com.artelier.api.entity.ItemBanner;

import java.util.UUID;

public record ItemBannerResponse(

        UUID id,
        boolean isActive,
        int sortOrder,
        String title,
        boolean showTitle,
        String backgroundImageUrl,
        String backgroundImageCloudinaryId,
        String model3dUrl,
        String model3dPublicId,
        String description,
        boolean showDescription,
        String buttonLabel,
        String buttonUrl,
        boolean showButton

) {

    public static ItemBannerResponse from(ItemBanner item) {
        return new ItemBannerResponse(
                item.getId(),
                item.isActive(),
                item.getSortOrder(),
                item.getTitle(),
                item.isShowTitle(),
                item.getBackgroundImageUrl(),
                item.getBackgroundImageCloudinaryId(),
                item.getModel3dUrl(),
                item.getModel3dPublicId(),
                item.getDescription(),
                item.isShowDescription(),
                item.getButtonLabel(),
                item.getButtonUrl(),
                item.isShowButton()
        );
    }
}