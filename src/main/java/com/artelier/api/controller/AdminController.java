package com.artelier.api.controller;

import com.artelier.api.dto.request.*;
import com.artelier.api.dto.response.*;
import com.artelier.api.dto.response.swagger.SwaggerResponses;
import com.artelier.api.service.BannerService;
import com.artelier.api.service.CategoryService;
import com.artelier.api.service.ProductService;
import com.artelier.api.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing the administrative API of Artelier.
 *
 * <p>All endpoints are mounted under {@code /admin}, require a valid JWT
 * (Bearer scheme) and are restricted to users holding the {@code ADMIN} role
 * through {@link PreAuthorize}.</p>
 *
 * <h2>Resource groups</h2>
 * <ul>
 *   <li><b>Users</b>: listing and ban management.</li>
 *   <li><b>Banners</b>: lifecycle of banners and image uploads.</li>
 *   <li><b>Banner items</b>: creation, ordering, update and removal of the
 *       items that compose a banner.</li>
 *   <li><b>Categories</b>: creation of product categories.</li>
 *   <li><b>Products</b>: creation, update, visibility toggle and soft delete.</li>
 * </ul>
 *
 * <h2>Response conventions</h2>
 * <ul>
 *   <li>Endpoints returning content wrap it in {@link AppResponse}
 *       ({@code success}, {@code message}, optional {@code data}).</li>
 *   <li>Endpoints that only change state answer {@code 204 No Content}
 *       with an empty body.</li>
 *   <li>Errors are translated to {@link AppResponse} with
 *       {@code success = false} by the global exception handler.</li>
 *   <li>All identifiers are UUIDs. A malformed UUID results in {@code 400}.</li>
 * </ul>
 *
 * <p>This class only handles HTTP concerns (routing, validation, status codes
 * and documentation). Business rules live in the service layer.</p>
 */
@RestController
@AllArgsConstructor
@RequestMapping("/admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    // =========================================================
    // OpenAPI shared descriptions
    // =========================================================

    private static final String DESC_UNAUTHORIZED =
            "Authentication required: the Bearer token is missing, expired or invalid";
    private static final String DESC_FORBIDDEN =
            "Access denied: the authenticated user does not have the ADMIN role";
    private static final String DESC_SERVER_ERROR =
            "Unexpected server error";
    private static final String DESC_INVALID_UUID =
            "Malformed UUID in the request path";

    private static final String TAG_USERS = "Admin - Users";
    private static final String TAG_BANNERS = "Admin - Banners";
    private static final String TAG_BANNER_ITEMS = "Admin - Banner items";
    private static final String TAG_CATEGORIES = "Admin - Categories";
    private static final String TAG_PRODUCTS = "Admin - Products";

    // =========================================================
    // Dependencies
    // =========================================================

    private final BannerService bannerService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final ProductService productService;

    // =========================================================
    // USERS
    // =========================================================

    /**
     * Bans a user so they can no longer use the application.
     *
     * @param id UUID of the user to ban
     * @return {@code 200} with a confirmation message
     */
    @PatchMapping("/users/{id}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_USERS)
    @Operation(
            operationId = "banUser",
            summary = "Ban a user",
            description = """
                    Marks the specified user as banned, preventing them from using the application.
                    The operation is idempotent: banning an already banned user succeeds.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "User banned successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "Banned", value = """
                            {
                              "success": true,
                              "message": "User banned"
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = DESC_INVALID_UUID,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "Invalid UUID", value = """
                            {
                              "success": false,
                              "message": "Invalid value for parameter 'id'"
                            }
                            """)
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "Not found", value = """
                            {
                              "success": false,
                              "message": "User not found"
                            }
                            """)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<AppResponse<Void>> banUser(
            @Parameter(
                    description = "UUID of the user to ban",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id
    ) {
        userService.setBanned(id, true);

        return ResponseEntity.ok(
                AppResponse.success("User banned")
        );
    }

    /**
     * Removes the ban from a user.
     *
     * @param id UUID of the user to unban
     * @return {@code 200} with a confirmation message
     */
    @PatchMapping("/users/{id}/unban")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_USERS)
    @Operation(
            operationId = "unbanUser",
            summary = "Unban a user",
            description = """
                    Removes the ban from the specified user so they can use the application again.
                    The operation is idempotent: unbanning a user who is not banned succeeds.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "User unbanned successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "Unbanned", value = """
                            {
                              "success": true,
                              "message": "User unbanned"
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = DESC_INVALID_UUID,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "Invalid UUID", value = """
                            {
                              "success": false,
                              "message": "Invalid value for parameter 'id'"
                            }
                            """)
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "Not found", value = """
                            {
                              "success": false,
                              "message": "User not found"
                            }
                            """)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<AppResponse<Void>> unbanUser(
            @Parameter(
                    description = "UUID of the user to unban",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id
    ) {
        userService.setBanned(id, false);

        return ResponseEntity.ok(
                AppResponse.success("User unbanned")
        );
    }

    /**
     * Returns a paginated list of users.
     *
     * @param page zero-based page index; must not be negative
     * @param size number of users per page; must be between 1 and 100
     * @return {@code 200} with a {@link Page} of {@link UserResponse}
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_USERS)
    @Operation(
            operationId = "listUsers",
            summary = "List users",
            description = """
                    Returns a paginated list of users.

                    - Page numbering starts at zero.
                    - Page size must be between 1 and 100.
                    - The response follows the Spring Data `Page` structure.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(name = "Page of users", value = """
                            {
                              "success": true,
                              "message": "Users retrieved",
                              "data": {
                                "content": [
                                  {
                                    "id": "550e8400-e29b-41d4-a716-446655440000",
                                    "email": "user@example.com",
                                    "fullName": "Alex Morgan",
                                    "phone": "+573001234567",
                                    "banned": false,
                                    "role": "USER"
                                  }
                                ],
                                "totalElements": 1,
                                "totalPages": 1,
                                "size": 20,
                                "number": 0,
                                "first": true,
                                "last": true,
                                "empty": false
                              }
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid pagination parameters",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "Negative page",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "Page index cannot be negative"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Invalid page size",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "Page size must be between 1 and 100"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Invalid parameter type",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "Invalid value for parameter 'page'"
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<AppResponse<Page<UserResponse>>> listUsers(
            @Parameter(
                    description = "Zero-based page index; must not be negative",
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = "integer", minimum = "0", defaultValue = "0"),
                    example = "0"
            )
            @RequestParam(defaultValue = "0") int page,

            @Parameter(
                    description = "Number of users per page; valid range: 1-100",
                    in = ParameterIn.QUERY,
                    schema = @Schema(
                            type = "integer",
                            minimum = "1",
                            maximum = "100",
                            defaultValue = "20"
                    ),
                    example = "20"
            )
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                AppResponse.success(
                        "Users retrieved",
                        userService.listUsers(page, size)
                )
        );
    }

    // =========================================================
    // BANNERS
    // =========================================================

    /**
     * Lists every banner, including inactive ones and their items.
     *
     * @return {@code 200} with the list of {@link BannerResponse}
     */
    @GetMapping("/banners")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_BANNERS)
    @Operation(
            operationId = "getAllBanners",
            summary = "List all banners",
            description = """
                    Returns all banners for administration purposes, including inactive
                    banners and their items. The public storefront uses a separate
                    endpoint that only exposes active content.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Banners retrieved successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            implementation = SwaggerResponses.BannerListResponseBody.class
                    ),
                    examples = @ExampleObject(name = "Banner list", value = """
                            {
                              "success": true,
                              "message": "Banners retrieved",
                              "data": [
                                {
                                  "id": "550e8400-e29b-41d4-a716-446655440000",
                                  "isActive": true,
                                  "createdAt": "2026-01-15T10:30:00Z",
                                  "items": []
                                }
                              ]
                            }
                            """)
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<AppResponse<List<BannerResponse>>> getAllBanners() {
        return ResponseEntity.ok(
                AppResponse.success(
                        "Banners retrieved",
                        bannerService.getAllBanners()
                )
        );
    }

    /**
     * Uploads a banner image to Cloudinary.
     *
     * <p>The returned URL and public ID are meant to be used afterwards when
     * creating or updating banner items.</p>
     *
     * @param file image file (JPG, PNG, WEBP or GIF), maximum 5 MB
     * @return {@code 201} with the uploaded asset data
     */
    @PostMapping(
            value = "/banners/uploads",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_BANNERS)
    @Operation(
            operationId = "uploadBannerImage",
            summary = "Upload a banner image",
            description = """
                    Uploads an image to Cloudinary and returns its secure URL and public ID.

                    - Supported formats: JPG, PNG, WEBP and GIF.
                    - Maximum file size: 5 MB.
                    - Send the image in the multipart part named `file`.

                    Use the returned `url` and `publicId` when creating or updating banner items.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Image uploaded successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            implementation = SwaggerResponses.UploadedAssetResponseBody.class
                    ),
                    examples = @ExampleObject(name = "Uploaded", value = """
                            {
                              "success": true,
                              "message": "Image uploaded",
                              "data": {
                                "url": "https://res.cloudinary.com/example/image/upload/banner.jpg",
                                "publicId": "artelier/banners/banner"
                              }
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid image file",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "Empty file",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "Image file cannot be empty"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "File too large",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "Each image must not exceed 5 MB"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Invalid media type",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "Only image files are allowed"
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error or failure while uploading to Cloudinary"
    )
    public ResponseEntity<AppResponse<UploadedAssetResponse>> uploadBannerImage(
            @Parameter(
                    description = "Image file (JPG, PNG, WEBP or GIF); maximum size 5 MB",
                    required = true
            )
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        AppResponse.success(
                                "Image uploaded",
                                bannerService.uploadImage(file)
                        )
                );
    }

    /**
     * Creates a banner together with its items.
     *
     * @param request banner data, containing between 1 and 20 items
     * @return {@code 201} with the created {@link BannerResponse}
     */
    @PostMapping("/banners")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_BANNERS)
    @Operation(
            operationId = "createBanner",
            summary = "Create a banner",
            description = """
                    Creates a banner with between 1 and 20 items.

                    Item fields are validated according to their visibility flags: when a
                    `show*` flag is enabled, the corresponding content field becomes required.
                    Images must be uploaded beforehand through `POST /admin/banners/uploads`.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Banner created successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            implementation = SwaggerResponses.BannerResponseBody.class
                    ),
                    examples = @ExampleObject(name = "Created banner", value = """
                            {
                              "success": true,
                              "message": "Banner created",
                              "data": {
                                "id": "550e8400-e29b-41d4-a716-446655440000",
                                "isActive": true,
                                "createdAt": "2026-01-15T10:30:00Z",
                                "items": []
                              }
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid banner data or validation failure",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "Validation failed", value = """
                            {
                              "success": false,
                              "message": "Validation failed",
                              "data": [
                                {
                                  "field": "items",
                                  "message": "must not be empty"
                                }
                              ]
                            }
                            """)
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<AppResponse<BannerResponse>> createBanner(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Banner data, including its items (1 to 20)",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BannerRequest.class)
                    )
            )
            @Valid @RequestBody BannerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        AppResponse.success(
                                "Banner created",
                                bannerService.createBanner(request)
                        )
                );
    }

    /**
     * Activates or deactivates a banner.
     *
     * @param bannerId UUID of the banner
     * @param request  body holding the new {@code isActive} value
     * @return {@code 204} with no content
     */
    @PatchMapping("/banners/{bannerId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_BANNERS)
    @Operation(
            operationId = "updateBannerStatus",
            summary = "Update banner status",
            description = """
                    Activates or deactivates the specified banner. Inactive banners are
                    kept in the database but are not shown in the storefront.
                    """
    )
    @ApiResponse(responseCode = "204", description = "Banner status updated successfully")
    @ApiResponse(
            responseCode = "400",
            description = "Malformed banner UUID or invalid request body",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(
            responseCode = "404",
            description = "Banner not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "Not found", value = """
                            {
                              "success": false,
                              "message": "Banner not found"
                            }
                            """)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<Void> updateBannerStatus(
            @Parameter(
                    description = "UUID of the banner",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID bannerId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New activation state of the banner",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = StatusRequest.class),
                            examples = @ExampleObject(name = "Deactivate", value = """
                                    {
                                      "isActive": false
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody StatusRequest request
    ) {
        bannerService.updateBannerStatus(bannerId, request.isActive());
        return ResponseEntity.noContent().build();
    }

    /**
     * Permanently deletes a banner.
     *
     * <p>The banner's Cloudinary assets are scheduled for deletion only after
     * the database transaction commits, so a rollback never leaves the banner
     * without its images.</p>
     *
     * @param bannerId UUID of the banner to delete
     * @return {@code 204} with no content
     */
    @DeleteMapping("/banners/{bannerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_BANNERS)
    @Operation(
            operationId = "deleteBanner",
            summary = "Delete a banner",
            description = """
                    Deletes the specified banner and all its items. Associated assets are
                    scheduled for deletion from Cloudinary after the database transaction
                    commits. This operation cannot be undone.
                    """
    )
    @ApiResponse(responseCode = "204", description = "Banner deleted successfully")
    @ApiResponse(
            responseCode = "400",
            description = DESC_INVALID_UUID,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(
            responseCode = "404",
            description = "Banner not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "Not found", value = """
                            {
                              "success": false,
                              "message": "Banner not found"
                            }
                            """)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<Void> deleteBanner(
            @Parameter(
                    description = "UUID of the banner to delete",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID bannerId
    ) {
        bannerService.deleteBanner(bannerId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // BANNER ITEMS
    // =========================================================

    /**
     * Appends an item at the end of a banner.
     *
     * @param bannerId UUID of the banner
     * @param request  item data
     * @return {@code 201} with the created {@link ItemBannerResponse}
     */
    @PostMapping("/banners/{bannerId}/items")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_BANNER_ITEMS)
    @Operation(
            operationId = "addBannerItem",
            summary = "Add an item to a banner",
            description = """
                    Adds an item at the end of the banner's ordered items.
                    Use `POST /admin/banners/{bannerId}/items/{order}` to insert at a specific position.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Item added successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            implementation = SwaggerResponses.ItemBannerResponseBody.class
                    ),
                    examples = @ExampleObject(name = "Created item", value = """
                            {
                              "success": true,
                              "message": "Item added",
                              "data": {
                                "id": "550e8400-e29b-41d4-a716-446655440001",
                                "isActive": true,
                                "sortOrder": 1,
                                "title": "Featured artwork",
                                "showTitle": true,
                                "backgroundImageUrl": "https://res.cloudinary.com/example/image/upload/banner.jpg",
                                "backgroundImageCloudinaryId": "artelier/banners/banner",
                                "model3dUrl": null,
                                "model3dPublicId": null,
                                "description": null,
                                "showDescription": false,
                                "buttonLabel": null,
                                "buttonUrl": null,
                                "showButton": false
                              }
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid item data, validation failure or malformed UUID",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(
            responseCode = "404",
            description = "Banner not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<AppResponse<ItemBannerResponse>> addBannerItem(
            @Parameter(
                    description = "UUID of the banner",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID bannerId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Item data",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ItemBannerRequest.class)
                    )
            )
            @Valid @RequestBody ItemBannerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        AppResponse.success(
                                "Item added",
                                bannerService.addItem(bannerId, request)
                        )
                );
    }

    /**
     * Inserts an item at a specific one-based position, shifting the
     * following items.
     *
     * @param bannerId UUID of the banner
     * @param order    one-based insertion position, from 1 to current item count + 1
     * @param request  item data
     * @return {@code 201} with the created {@link ItemBannerResponse}
     */
    @PostMapping("/banners/{bannerId}/items/{order}")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_BANNER_ITEMS)
    @Operation(
            operationId = "insertBannerItem",
            summary = "Insert an item at a specific position",
            description = """
                    Inserts a new item at the requested one-based position.

                    - The valid insertion range is 1 through the current item count plus one.
                    - Existing items are reordered as needed.
                    - A banner can hold at most 20 items.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Item inserted successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            implementation = SwaggerResponses.ItemBannerResponseBody.class
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid insertion position, item data or malformed UUID",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "Invalid position", value = """
                            {
                              "success": false,
                              "message": "Insertion order must be between 1 and 4"
                            }
                            """)
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(
            responseCode = "404",
            description = "Banner not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<AppResponse<ItemBannerResponse>> insertBannerItem(
            @Parameter(
                    description = "UUID of the banner",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID bannerId,

            @Parameter(
                    description = "One-based insertion position (1 to current item count + 1)",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = @Schema(type = "integer", minimum = "1"),
                    example = "1"
            )
            @PathVariable int order,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Item data",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ItemBannerRequest.class)
                    )
            )
            @Valid @RequestBody ItemBannerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        AppResponse.success(
                                "Item added",
                                bannerService.addItem(bannerId, request, order)
                        )
                );
    }

    /**
     * Updates the content and visibility settings of a banner item.
     *
     * @param bannerId UUID of the banner
     * @param itemId   UUID of the item to update
     * @param request  new item data
     * @return {@code 200} with the updated {@link ItemBannerResponse}
     */
    @PutMapping("/banners/{bannerId}/items/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_BANNER_ITEMS)
    @Operation(
            operationId = "updateBannerItem",
            summary = "Update a banner item",
            description = """
                    Updates the content and visibility settings of a banner item.
                    The item's position and active status are not modified by this endpoint;
                    use the `position` and `status` endpoints for that.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Item updated successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            implementation = SwaggerResponses.ItemBannerResponseBody.class
                    ),
                    examples = @ExampleObject(name = "Updated item", value = """
                            {
                              "success": true,
                              "message": "Item updated",
                              "data": {
                                "id": "550e8400-e29b-41d4-a716-446655440001",
                                "isActive": true,
                                "sortOrder": 1,
                                "title": "Updated artwork",
                                "showTitle": true,
                                "backgroundImageUrl": "https://res.cloudinary.com/example/image/upload/banner.jpg",
                                "backgroundImageCloudinaryId": "artelier/banners/banner",
                                "model3dUrl": null,
                                "model3dPublicId": null,
                                "description": null,
                                "showDescription": false,
                                "buttonLabel": null,
                                "buttonUrl": null,
                                "showButton": false
                              }
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid item data, validation failure or malformed UUID",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(
            responseCode = "404",
            description = "Banner or banner item not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<AppResponse<ItemBannerResponse>> updateBannerItem(
            @Parameter(
                    description = "UUID of the banner",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID bannerId,

            @Parameter(
                    description = "UUID of the item to update",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "550e8400-e29b-41d4-a716-446655440001"
            )
            @PathVariable UUID itemId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New item data",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ItemBannerUpdateRequest.class)
                    )
            )
            @Valid @RequestBody ItemBannerUpdateRequest request
    ) {
        return ResponseEntity.ok(
                AppResponse.success(
                        "Item updated",
                        bannerService.updateItem(bannerId, itemId, request)
                )
        );
    }

    /**
     * Activates or deactivates a banner item.
     *
     * @param bannerId UUID of the banner
     * @param itemId   UUID of the item
     * @param request  body holding the new {@code isActive} value
     * @return {@code 204} with no content
     */
    @PatchMapping("/banners/{bannerId}/items/{itemId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_BANNER_ITEMS)
    @Operation(
            operationId = "updateBannerItemStatus",
            summary = "Update banner item status",
            description = """
                    Activates or deactivates a banner item. Inactive items keep their
                    position but are hidden from the storefront.
                    """
    )
    @ApiResponse(responseCode = "204", description = "Item status updated successfully")
    @ApiResponse(
            responseCode = "400",
            description = "Malformed UUID or invalid request body",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(
            responseCode = "404",
            description = "Banner or banner item not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<Void> updateBannerItemStatus(
            @Parameter(
                    description = "UUID of the banner",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID bannerId,

            @Parameter(
                    description = "UUID of the banner item",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "550e8400-e29b-41d4-a716-446655440001"
            )
            @PathVariable UUID itemId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New activation state of the item",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = StatusRequest.class),
                            examples = @ExampleObject(name = "Activate", value = """
                                    {
                                      "isActive": true
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody StatusRequest request
    ) {
        bannerService.updateItemStatus(bannerId, itemId, request.isActive());
        return ResponseEntity.noContent().build();
    }

    /**
     * Moves an item to a new one-based position within its banner.
     *
     * @param bannerId UUID of the banner
     * @param itemId   UUID of the item to move
     * @param request  body holding the new position ({@code newOrder})
     * @return {@code 204} with no content
     */
    @PatchMapping("/banners/{bannerId}/items/{itemId}/position")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_BANNER_ITEMS)
    @Operation(
            operationId = "moveBannerItem",
            summary = "Move a banner item",
            description = """
                    Moves an item to a new one-based position.
                    The new position must be between 1 and the total number of items;
                    the remaining items are reordered accordingly.
                    """
    )
    @ApiResponse(responseCode = "204", description = "Item position updated successfully")
    @ApiResponse(
            responseCode = "400",
            description = "Malformed UUID, missing position or position out of range",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(
            responseCode = "404",
            description = "Banner or banner item not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<Void> moveBannerItem(
            @Parameter(
                    description = "UUID of the banner",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID bannerId,

            @Parameter(
                    description = "UUID of the item to move",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "550e8400-e29b-41d4-a716-446655440001"
            )
            @PathVariable UUID itemId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New one-based position of the item",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PositionRequest.class),
                            examples = @ExampleObject(name = "Move to position 2", value = """
                                    {
                                      "newOrder": 2
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody PositionRequest request
    ) {
        bannerService.moveItem(bannerId, itemId, request.newOrder());
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes an item from a banner and normalizes the remaining positions.
     *
     * @param bannerId UUID of the banner
     * @param itemId   UUID of the item to delete
     * @return {@code 204} with no content
     */
    @DeleteMapping("/banners/{bannerId}/items/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_BANNER_ITEMS)
    @Operation(
            operationId = "deleteBannerItem",
            summary = "Delete a banner item",
            description = """
                    Removes the item from its banner and normalizes the positions
                    of the remaining items. Associated assets are scheduled for
                    deletion after the database transaction commits.
                    This operation cannot be undone.
                    """
    )
    @ApiResponse(responseCode = "204", description = "Item deleted successfully")
    @ApiResponse(
            responseCode = "400",
            description = DESC_INVALID_UUID,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(
            responseCode = "404",
            description = "Banner or banner item not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<Void> deleteBannerItem(
            @Parameter(
                    description = "UUID of the banner",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID bannerId,

            @Parameter(
                    description = "UUID of the item to delete",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "550e8400-e29b-41d4-a716-446655440001"
            )
            @PathVariable UUID itemId
    ) {
        bannerService.deleteItem(bannerId, itemId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // CATEGORIES
    // =========================================================

    /**
     * Creates a product category.
     *
     * @param request category data (name and unique slug)
     * @return {@code 201} with the created {@link CategoryResponse}
     */
    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_CATEGORIES)
    @Operation(
            operationId = "createCategory",
            summary = "Create a category",
            description = """
                    Creates a product category.

                    - `name` and `slug` are required.
                    - `slug` must be unique across all categories; otherwise `409` is returned.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Category created successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            implementation = SwaggerResponses.CategoryResponseBody.class
                    ),
                    examples = @ExampleObject(name = "Created category", value = """
                            {
                              "success": true,
                              "message": "Category created",
                              "data": {
                                "id": "550e8400-e29b-41d4-a716-446655440000",
                                "name": "Ceramics",
                                "slug": "ceramics",
                                "description": "Handmade ceramic products including mugs and bowls"
                              }
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid category data",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "Validation failed", value = """
                            {
                              "success": false,
                              "message": "Validation failed",
                              "data": [
                                {
                                  "field": "name",
                                  "message": "Name is required"
                                }
                              ]
                            }
                            """)
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(
            responseCode = "409",
            description = "The category slug is already in use",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "Duplicate slug", value = """
                            {
                              "success": false,
                              "message": "Slug already in use"
                            }
                            """)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<AppResponse<CategoryResponse>> createCategory(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Category data",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CategoryRequest.class)
                    )
            )
            @Valid @RequestBody CategoryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        AppResponse.success(
                                "Category created",
                                categoryService.create(request)
                        )
                );
    }

    // =========================================================
    // PRODUCTS
    // =========================================================

    /**
     * Flips the {@code isActive} flag of a product.
     *
     * @param id UUID of the product
     * @return {@code 200} with the updated {@link ProductResponse}
     */
    @PatchMapping("/products/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_PRODUCTS)
    @Operation(
            operationId = "toggleProductActive",
            summary = "Toggle product visibility",
            description = """
                    Switches the product's `isActive` value to its opposite and returns
                    the updated product. This operation is **not** idempotent: each call
                    flips the current state.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product visibility updated successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            implementation = SwaggerResponses.ProductResponseBody.class
                    ),
                    examples = @ExampleObject(name = "Hidden product", value = """
                            {
                              "success": true,
                              "message": "Product visibility updated",
                              "data": {
                                "id": "550e8400-e29b-41d4-a716-446655440000",
                                "name": "Handmade Ceramic Mug",
                                "slug": "handmade-ceramic-mug",
                                "price": 49.99,
                                "isActive": false
                              }
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = DESC_INVALID_UUID,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "Not found", value = """
                            {
                              "success": false,
                              "message": "Product not found"
                            }
                            """)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<AppResponse<ProductResponse>> toggleProductActive(
            @Parameter(
                    description = "UUID of the product",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                AppResponse.success(
                        "Product visibility updated",
                        productService.toggleActive(id)
                )
        );
    }

    /**
     * Soft-deletes a product by setting its {@code deletedAt} timestamp.
     *
     * @param id UUID of the product to delete
     * @return {@code 204} with no content
     */
    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_PRODUCTS)
    @Operation(
            operationId = "deleteProduct",
            summary = "Delete a product",
            description = """
                    Soft-deletes the product by setting its `deletedAt` timestamp.
                    The product record is retained in the database and is excluded
                    from catalog queries.
                    """
    )
    @ApiResponse(responseCode = "204", description = "Product deleted successfully")
    @ApiResponse(
            responseCode = "400",
            description = DESC_INVALID_UUID,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "Not found", value = """
                            {
                              "success": false,
                              "message": "Product not found"
                            }
                            """)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<Void> deleteProduct(
            @Parameter(
                    description = "UUID of the product to delete",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id
    ) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Updates a product using a multipart request.
     *
     * @param id      UUID of the product to update
     * @param request product fields, sent as the JSON part {@code data}
     * @param images  optional image files, sent as the part {@code images}
     * @return {@code 200} with the updated {@link ProductResponse}
     */
    @PutMapping(
            value = "/products/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_PRODUCTS)
    @Operation(
            operationId = "updateProduct",
            summary = "Update a product",
            description = """
                    Updates a product using a `multipart/form-data` request with two parts:

                    - `data` (required): JSON object with the product fields
                      (content type `application/json`).
                    - `images` (optional): one or more image files.

                    If no new images are provided, the existing images are preserved.
                    Updating the name regenerates the product slug.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product updated successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            implementation = SwaggerResponses.ProductResponseBody.class
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid product data, malformed UUID or malformed multipart request",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(
            responseCode = "404",
            description = "Product or category not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "Not found", value = """
                            {
                              "success": false,
                              "message": "Product not found"
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error or failure while uploading images"
    )
    public ResponseEntity<AppResponse<ProductResponse>> updateProduct(
            @Parameter(
                    description = "UUID of the product to update",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id,

            @Parameter(
                    description = "Product fields as a JSON object (multipart part `data`)",
                    required = true,
                    schema = @Schema(implementation = ProductRequest.class)
            )
            @RequestPart("data") @Valid ProductRequest request,

            @Parameter(
                    description = "Optional image files to associate with the product (multipart part `images`)"
            )
            @RequestPart(value = "images", required = false)
            List<MultipartFile> images
    ) {
        return ResponseEntity.ok(
                AppResponse.success(
                        "Product updated successfully",
                        productService.updateProduct(id, request, images)
                )
        );
    }

    /**
     * Creates a product using a multipart request.
     *
     * @param request product fields, sent as the JSON part {@code data}
     * @param images  optional image files, sent as the part {@code images}
     * @return {@code 201} with the created {@link ProductResponse}
     */
    @PostMapping(
            value = "/products",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = TAG_PRODUCTS)
    @Operation(
            operationId = "createProduct",
            summary = "Create a product",
            description = """
                    Creates a product using a `multipart/form-data` request with two parts:

                    - `data` (required): JSON object with the product fields
                      (content type `application/json`).
                    - `images` (optional): one or more image files.

                    The referenced category must exist.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Product created successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            implementation = SwaggerResponses.ProductResponseBody.class
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid product data or malformed multipart request",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(responseCode = "401", description = DESC_UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = DESC_FORBIDDEN)
    @ApiResponse(
            responseCode = "404",
            description = "Category not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "Category not found", value = """
                            {
                              "success": false,
                              "message": "Category not found"
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error or failure while uploading images"
    )
    public ResponseEntity<AppResponse<ProductResponse>> createProduct(
            @Parameter(
                    description = "Product fields as a JSON object (multipart part `data`)",
                    required = true,
                    schema = @Schema(implementation = ProductRequest.class)
            )
            @RequestPart("data") @Valid ProductRequest request,

            @Parameter(
                    description = "Optional image files to associate with the product (multipart part `images`)"
            )
            @RequestPart(value = "images", required = false)
            List<MultipartFile> images
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        AppResponse.success(
                                "Product created successfully",
                                productService.createProduct(request, images)
                        )
                );
    }
}