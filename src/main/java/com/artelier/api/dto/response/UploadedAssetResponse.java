
package com.artelier.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "UploadedAssetResponse",
        description = "Result of uploading an asset to Cloudinary"
)
public record UploadedAssetResponse(

        @Schema(
                description = "Public URL of the uploaded asset",
                example = "https://res.cloudinary.com/artelier/image/upload/banners/banner.jpg",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String url,

        @Schema(
                description = "Cloudinary public identifier used to manage or delete the asset",
                example = "banners/banner",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String publicId

) {}
