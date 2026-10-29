
package com.artelier.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Standard envelope that wraps every JSON response of the API.
 *
 * @param <T> type of the response payload
 */
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
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean success;

    @Schema(
            description = "Human-readable message describing the result",
            example = "Product created successfully",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String message;

    @Schema(
            description = "Response payload containing requested data",
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
