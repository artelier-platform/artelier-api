package com.artelier.api.integration.cloudinary.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudinaryUploadResponse {

    @JsonProperty("public_id")
    private String publicId;

    @JsonProperty("secure_url")
    private String secureUrl;

    @JsonProperty("format")
    private String format;

    @JsonProperty("width")
    private Integer width;

    @JsonProperty("height")
    private Integer height;

    @JsonProperty("bytes")
    private Long bytes;

    @JsonProperty("resource_type")
    private String resourceType;

    @JsonProperty("asset_id")
    private String assetId;
}