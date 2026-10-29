package com.artelier.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/**
 * Request payload to update a single item of a banner.
 *
 * <p>Same fields and cross-field rules as {@link ItemBannerRequest}, except
 * for {@code sortOrder}: the position of an item is changed through
 * {@link PositionRequest}.</p>
 *
 * @param title                       title text (up to 120 characters)
 * @param showTitle                   whether the title is displayed
 * @param backgroundImageUrl          URL of the background image (up to 2048 characters)
 * @param backgroundImageCloudinaryId Cloudinary public identifier of the background image
 * @param model3dUrl                  URL of the 3D model (up to 2048 characters)
 * @param model3dPublicId             Cloudinary public identifier of the 3D model
 * @param description                 description text (up to 300 characters)
 * @param showDescription             whether the description is displayed
 * @param buttonLabel                 button label (up to 40 characters)
 * @param buttonUrl                   button target URL (up to 500 characters)
 * @param showButton                  whether the button is displayed
 */
@Schema(description = "Update payload for a single item of a banner")
public record ItemBannerUpdateRequest(

        @Schema(
                description = "Title text. Required when showTitle is true.",
                example = "Handmade Ceramics",
                maxLength = 120
        )
        @Size(max = 120)
        String title,

        @Schema(description = "Whether the title is displayed", example = "true")
        boolean showTitle,

        @Schema(
                description = "URL of the background image. At least one of backgroundImageUrl or model3dUrl is required.",
                example = "https://res.cloudinary.com/demo/image/upload/banner.jpg",
                maxLength = 2048
        )
        @Size(max = 2048)
        String backgroundImageUrl,

        @Schema(
                description = "Cloudinary public identifier of the background image",
                example = "artelier/banners/banner"
        )
        String backgroundImageCloudinaryId,

        @Schema(
                description = "URL of the 3D model. At least one of backgroundImageUrl or model3dUrl is required.",
                example = "https://res.cloudinary.com/demo/raw/upload/model.glb",
                maxLength = 2048
        )
        @Size(max = 2048)
        String model3dUrl,

        @Schema(
                description = "Cloudinary public identifier of the 3D model",
                example = "artelier/banners/model"
        )
        String model3dPublicId,

        @Schema(
                description = "Description text. Required when showDescription is true.",
                example = "Pieces crafted by local artisans",
                maxLength = 300
        )
        @Size(max = 300)
        String description,

        @Schema(description = "Whether the description is displayed", example = "true")
        boolean showDescription,

        @Schema(
                description = "Button label. Required when showButton is true.",
                example = "Shop now",
                maxLength = 40
        )
        @Size(max = 40)
        String buttonLabel,

        @Schema(
                description = "Button target URL. Required when showButton is true.",
                example = "https://artelier-web.vercel.app/products",
                maxLength = 500
        )
        @Size(max = 500)
        String buttonUrl,

        @Schema(description = "Whether the button is displayed", example = "true")
        boolean showButton

) {

    /**
     * Validates that at least one background source is provided.
     *
     * @return {@code true} when an image URL or a 3D model URL is present
     */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "At least one background source must be provided")
    public boolean isBackgroundSourceValid() {
        return hasText(backgroundImageUrl) || hasText(model3dUrl);
    }

    /**
     * Validates that a title is present when it is meant to be displayed.
     *
     * @return {@code true} when {@code showTitle} is disabled or a title is present
     */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "A title is required when showTitle is enabled")
    public boolean isTitleValid() {
        return !showTitle || hasText(title);
    }

    /**
     * Validates that a description is present when it is meant to be displayed.
     *
     * @return {@code true} when {@code showDescription} is disabled or a description is present
     */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "A description is required when showDescription is enabled")
    public boolean isDescriptionValid() {
        return !showDescription || hasText(description);
    }

    /**
     * Validates that the button data is complete when the button is meant to be displayed.
     *
     * @return {@code true} when {@code showButton} is disabled or both label and URL are present
     */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Button label and URL are required when showButton is enabled")
    public boolean isButtonValid() {
        return !showButton || (hasText(buttonLabel) && hasText(buttonUrl));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
