package com.artelier.api.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record ItemBannerUpdateRequest(

        @Size(max = 120)
        String title,

        boolean showTitle,

        @Size(max = 2048)
        String backgroundImageUrl,

        String backgroundImageCloudinaryId,

        @Size(max = 2048)
        String model3dUrl,

        String model3dPublicId,

        @Size(max = 300)
        String description,

        boolean showDescription,

        @Size(max = 40)
        String buttonLabel,

        @Size(max = 500)
        String buttonUrl,

        boolean showButton

) {

    @AssertTrue(message = "At least one background source must be provided")
    public boolean isBackgroundSourceValid() {
        return hasText(backgroundImageUrl) || hasText(model3dUrl);
    }

    @AssertTrue(message = "A title is required when showTitle is enabled")
    public boolean isTitleValid() {
        return !showTitle || hasText(title);
    }

    @AssertTrue(message = "A description is required when showDescription is enabled")
    public boolean isDescriptionValid() {
        return !showDescription || hasText(description);
    }

    @AssertTrue(message = "Button label and URL are required when showButton is enabled")
    public boolean isButtonValid() {
        return !showButton || (hasText(buttonLabel) && hasText(buttonUrl));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}