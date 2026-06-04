package com.artelier.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "ApiResponse",
        description = "Standard API response wrapper used for all endpoints"
)
public class AppResponse<T> {

    @Schema(
            description = "Indicates whether the request was successful",
            example = "true"
    )
    private boolean success;

    @Schema(
            description = "Human-readable message describing the result",
            example = "Product created successfully"
    )
    private String message;

    @Schema(
            description = "Response payload containing requested data (nullable when no data is returned)",
            nullable = true
    )
    private T data;

    public static <T> AppResponse<T> success(String message, T data) {
        return new AppResponse<>(true, message, data);
    }

    public static <T> AppResponse<T> success(String message) {
        return new AppResponse<>(true, message, null);
    }

    public static <T> AppResponse<T> error(String message) {
        return new AppResponse<>(false, message, null);
    }
}