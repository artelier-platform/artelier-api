
package com.artelier.api.dto.response;

import com.artelier.api.entity.ItemBanner;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
        name = "ItemBannerResponse",
        description = "A single item of a banner"
)
public record ItemBannerResponse(

        @Schema(
                description = "Unique identifier of the item",
                example = "550e8400-e29b-41d4-a716-446655440001",
                format = "uuid",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Whether the item is active",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean isActive,

        @Schema(
                description = "Position of the item inside the banner, starting at 1",
                example = "1",
                minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        int sortOrder,

        @Schema(
                description = "Title text",
                example = "Handmade Ceramics",
                nullable = true
        )
        String title,

        @Schema(
                description = "Whether the title is displayed",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean showTitle,

        @Schema(
                description = "URL of the background image",
                example = "https://res.cloudinary.com/demo/image/upload/banner.jpg",
                nullable = true
        )
        String backgroundImageUrl,

        @Schema(
                description = "Cloudinary public identifier of the background image",
                example = "artelier/banners/banner",
                nullable = true
        )
        String backgroundImageCloudinaryId,

        @Schema(
                description = "URL of the 3D model",
                example = "https://res.cloudinary.com/demo/raw/upload/model.glb",
                nullable = true
        )
        String model3dUrl,

        @Schema(
                description = "Cloudinary public identifier of the 3D model",
                example = "artelier/banners/model",
                nullable = true
        )
        String model3dPublicId,

        @Schema(
                description = "Description text",
                example = "Pieces crafted by local artisans",
                nullable = true
        )
        String description,

        @Schema(
                description = "Whether the description is displayed",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean showDescription,

        @Schema(
                description = "Button label",
                example = "Shop now",
                nullable = true
        )
        String buttonLabel,

        @Schema(
                description = "Button target URL",
                example = "https://artelier-web.vercel.app/products",
                nullable = true
        )
        String buttonUrl,

        @Schema(
                description = "Whether the button is displayed",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
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
