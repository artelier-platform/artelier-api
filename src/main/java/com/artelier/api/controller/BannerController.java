package com.artelier.api.controller;

import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.BannerResponse;
import com.artelier.api.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing the banners displayed to visitors of the storefront.
 *
 * <p>All endpoints in this controller are <b>public</b>: they do not require a
 * Bearer token. They are explicitly excluded from the global {@code bearerAuth}
 * security requirement through {@link SecurityRequirements}, so Swagger UI does
 * not send an {@code Authorization} header for them.</p>
 *
 * <h2>Response conventions</h2>
 * <ul>
 *   <li>Every response is wrapped in {@link AppResponse}
 *       ({@code success}, {@code message}, optional {@code data}).</li>
 *   <li>Errors are translated to {@link AppResponse} with {@code success = false}
 *       by the global exception handler.</li>
 * </ul>
 *
 * <p>This class only handles HTTP concerns (routing, status codes and
 * documentation). The logic that decides which banners are active lives in
 * {@link BannerService}.</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(
        name = "Banners",
        description = """
        Public endpoints to retrieve the banners shown to visitors.

        ## Access

        These endpoints are **public**. No authentication is required and no
        `Authorization` header must be sent.

        ## Response Format

        Every response is wrapped in the standard `AppResponse` envelope:

        | Field | Description |
        |-------|-------------|
        | `success` | `true` when the request was processed correctly |
        | `message` | Human-readable summary of the result |
        | `data` | Payload of the response (when applicable) |
        """
)
public class BannerController {

    // =========================================================
    // OpenAPI shared descriptions
    // =========================================================

    private static final String DESC_SERVER_ERROR = "Unexpected server error";

    // =========================================================
    // Dependencies
    // =========================================================

    private final BannerService bannerService;

    // =========================================================
    // PUBLIC
    // =========================================================

    /**
     * Retrieves the banners that are currently active.
     *
     * <p>The criteria that determine whether a banner is active, as well as the
     * ordering of the result, are defined by {@link BannerService}.</p>
     *
     * @return {@code 200} with the list of active banners; the list is empty
     *         when there are none
     */
    @SecurityRequirements
    @Operation(
            operationId = "getActiveBanners",
            summary = "List active banners",
            description = """
        Retrieves the banners that are currently active.

        ## Access

        This endpoint is public and does not require authentication.

        ## Result

        - Only active banners are returned.
        - When there are no active banners, the endpoint still responds
          with `200` and an empty `data` list.
        """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Active banners retrieved successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "banners-empty",
                            summary = "No active banners",
                            value = """
                                {
                                  "success": true,
                                  "message": "Banners retrieved",
                                  "data": []
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "500",
            description = DESC_SERVER_ERROR,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @GetMapping("/banners")
    public ResponseEntity<AppResponse<List<BannerResponse>>> getActiveBanners() {
        return ResponseEntity.ok(AppResponse.success(
                "Banners retrieved",
                bannerService.getActiveBanners()
        ));
    }
}