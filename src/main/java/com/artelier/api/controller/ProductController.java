package com.artelier.api.controller;

import com.artelier.api.dto.request.ProductRequest;
import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.ProductResponse;
import com.artelier.api.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/products")
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Products",
        description = """
        Product catalog management for the Artelier marketplace.
        
        ## Access Control
        
        | Endpoint | Public | Buyer | Admin |
        |----------|--------|-------|-------|
        | `GET /products` | ✅ | ✅ | ✅ |
        | `GET /products/{slug}` | ✅ | ✅ | ✅ |
        | `POST /products` | ❌ | ❌ | ✅ |
        | `PUT /products/{id}` | ❌ | ❌ | ✅ |
        | `DELETE /products/{id}` | ❌ | ❌ | ✅ |
        | `PATCH /products/{id}/toggle` | ❌ | ❌ | ✅ |
        
        ## Image Upload
        
        Create and update endpoints use `multipart/form-data` with two named parts:
        - **`data`** — product JSON (`ProductRequest`) including per-image metadata (`isPrimary`, `sortOrder`)
        - **`images`** — one or more image files
        
        Files and metadata are coordinated by index: `images[0]` → `data.images[0]`, etc.
        
        ## Stock Types
        
        | Type | Behavior |
        |------|----------|
        | `UNLIMITED` | No stock tracking — always available |
        | `AVAILABLE` | Stock is tracked and decremented on order |
        | `MADE_TO_ORDER` | Stock is tracked but items are produced on demand |
        
        ## Slugs
        
        Slugs are auto-generated from the product name and guaranteed unique.
        Updating the name regenerates the slug.
        """
)
public class ProductController {

    private final ProductService productService;

    // ──────────────────────────────────────────────────────────────────────────
    // GET /products — public
    // ──────────────────────────────────────────────────────────────────────────
    @GetMapping
    @SecurityRequirements
    @Operation(
            summary = "List products",
            description = """
            Returns a paginated list of active products, optionally filtered by category.
            
            Results are cached by `categoryId + page + size + sort`.
            The cache is evicted on any create, update, delete, or toggle operation.
            
            ## Pagination defaults
            
            | Parameter | Default |
            |-----------|---------|
            | `page` | `0` |
            | `size` | `20` |
            | `sort` | unsorted |
            
            ## Sort examples
            
            - `?sort=price,asc` — cheapest first
            - `?sort=name,asc` — alphabetical
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Products retrieved",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "product-page",
                            value = """
                                    {
                                      "success": true,
                                      "message": "Products retrieved",
                                      "data": {
                                        "content": [
                                          {
                                            "id": "550e8400-e29b-41d4-a716-446655440000",
                                            "category": {
                                              "id": "550e8400-e29b-41d4-a716-446655440111",
                                              "name": "Ceramics",
                                              "slug": "ceramics",
                                              "description": "Handmade ceramic products including mugs and bowls"
                                            },
                                            "name": "Handmade Ceramic Mug",
                                            "slug": "handmade-ceramic-mug",
                                            "description": "Beautiful handmade mug",
                                            "story": "Crafted by local artisans using traditional techniques",
                                            "price": 49.99,
                                            "stockType": "AVAILABLE",
                                            "stockQuantity": 10,
                                            "isCustomOrder": false,
                                            "isActive": true,
                                            "createdAt": "2026-01-15T10:30:00Z",
                                            "images": [
                                              {
                                                "id": "550e8400-e29b-41d4-a716-446655440001",
                                                "url": "https://res.cloudinary.com/artelier/image/upload/v1/artelier/products/abc123.jpg",
                                                "cloudinaryId": "artelier/products/abc123",
                                                "isPrimary": true,
                                                "sortOrder": 0
                                              }
                                            ]
                                          }
                                        ],
                                        "totalElements": 42,
                                        "totalPages": 3,
                                        "first": true,
                                        "last": false,
                                        "numberOfElements": 20,
                                        "empty": false
                                      }
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<AppResponse<Page<ProductResponse>>> getAllProducts(
            @Parameter(
                    description = "Filter by category UUID. Omit to return all categories.",
                    example = "660e8400-e29b-41d4-a716-446655440001"
            )
            @RequestParam(required = false) UUID categoryId,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(
                AppResponse.success(
                        "Products retrieved",
                        productService.getAllProducts(categoryId, pageable)
                )
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /products/{slug} — public
    // ──────────────────────────────────────────────────────────────────────────
    @GetMapping("/{slug}")
    @SecurityRequirements
    @Operation(
            summary = "Get product by slug",
            description = """
            Returns a single product identified by its URL-friendly slug.
            
            Slugs are derived from the product name and are guaranteed unique.
            Example: product name `"Handmade Ceramic Mug"` → slug `"handmade-ceramic-mug"`.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "product-detail",
                            value = """
                                    {
                                      "success": true,
                                      "message": "Product retrieved",
                                      "data": {
                                        "id": "550e8400-e29b-41d4-a716-446655440000",
                                        "category": {
                                          "id": "550e8400-e29b-41d4-a716-446655440111",
                                          "name": "Ceramics",
                                          "slug": "ceramics",
                                          "description": "Handmade ceramic products including mugs and bowls"
                                        },
                                        "name": "Handmade Ceramic Mug",
                                        "slug": "handmade-ceramic-mug",
                                        "description": "Beautiful handmade mug",
                                        "story": "Crafted by local artisans in Bogotá...",
                                        "price": 49.99,
                                        "stockType": "AVAILABLE",
                                        "stockQuantity": 10,
                                        "isCustomOrder": false,
                                        "isActive": true,
                                        "createdAt": "2026-01-15T10:30:00Z",
                                        "images": [
                                          {
                                            "id": "550e8400-e29b-41d4-a716-446655440001",
                                            "url": "https://res.cloudinary.com/artelier/image/upload/v1/artelier/products/abc123.jpg",
                                            "cloudinaryId": "artelier/products/abc123",
                                            "isPrimary": true,
                                            "sortOrder": 0
                                          },
                                          {
                                            "id": "550e8400-e29b-41d4-a716-446655440002",
                                            "url": "https://res.cloudinary.com/artelier/image/upload/v1/artelier/products/def456.jpg",
                                            "cloudinaryId": "artelier/products/def456",
                                            "isPrimary": false,
                                            "sortOrder": 1
                                          }
                                        ]
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "message": "Product not found"
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<AppResponse<ProductResponse>> getBySlug(
            @Parameter(
                    description = "URL-friendly product identifier",
                    example = "handmade-ceramic-mug"
            )
            @PathVariable String slug
    ) {
        return ResponseEntity.ok(
                AppResponse.success("Product retrieved", productService.getBySlug(slug))
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /products — ADMIN only, multipart/form-data
    // ──────────────────────────────────────────────────────────────────────────
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create product",
            description = """
            Creates a new product and uploads the provided images to Cloudinary.
            
            ## Request format
            
            This endpoint uses `multipart/form-data` with two named parts:
            
            | Part | Type | Description |
            |------|------|-------------|
            | `data` | `application/json` | Product fields + per-image metadata |
            | `images` | `image/*` (multiple) | Image files to upload |
            
            Files and metadata are matched by index: `images[0]` corresponds to `data.images[0]`.
            
            If `data.images` is absent or shorter than the files list, defaults apply:
            - First image → `isPrimary = true`
            - `sortOrder` → file position index
            
            ## Slug generation
            
            The slug is auto-generated from `name`. Special characters are stripped and
            spaces are replaced with hyphens. If the slug already exists, a numeric
            suffix is appended (`handmade-ceramic-mug-2`, etc.).
            
            ## Example (JavaScript)
            
            ```javascript
            const formData = new FormData();
            formData.append('data', new Blob([JSON.stringify({
              categoryId: '660e8400-...',
              name: 'Handmade Ceramic Mug',
              price: 49.99,
              stockType: 'AVAILABLE',
              stockQuantity: 10,
              isCustomOrder: false,
              isActive: true,
              images: [
                { isPrimary: true, sortOrder: 0 },
                { isPrimary: false, sortOrder: 1 }
              ]
            })], { type: 'application/json' }));
            formData.append('images', mugFrontFile);
            formData.append('images', mugSideFile);
            ```
            """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Product created",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "product-created",
                            value = """
                                    {
                                      "success": true,
                                      "message": "Product created successfully",
                                      "data": {
                                        "id": "550e8400-e29b-41d4-a716-446655440000",
                                        "name": "Handmade Ceramic Mug",
                                        "slug": "handmade-ceramic-mug",
                                        "description": "Beautiful handmade mug",
                                        "price": 49.99,
                                        "stockType": "AVAILABLE",
                                        "stockQuantity": 10,
                                        "isActive": true,
                                        "isCustomOrder": false,
                                        "images": [
                                          {
                                            "url": "https://res.cloudinary.com/artelier/image/upload/v1/artelier/products/abc123.jpg",
                                            "cloudinaryId": "artelier/products/abc123",
                                            "isPrimary": true,
                                            "sortOrder": 0
                                          }
                                        ]
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "message": "Validation failed",
                                      "data": [
                                        { "field": "price", "message": "must be greater than 0" },
                                        { "field": "name", "message": "must not be blank" }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Category not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "message": "Category not found"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Not authenticated",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                    "success": false,
                                    "message": "Unauthorized"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Authenticated but does not have ADMIN privileges",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                    "success": false,
                                    "message": "Access Denied"
                                    }
                                    """
                    )
            )
    )

    @ApiResponse(
            responseCode = "500",
            description = "Image upload to Cloudinary failed",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "message": "Image upload failed. Please try again later."
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<AppResponse<ProductResponse>> createProduct(
            @Parameter(description = "Product data as JSON", required = true)
            @RequestPart("data") @Valid ProductRequest request,
            @Parameter(description = "Product image files (coordinated by index with data.images)")
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AppResponse.success(
                        "Product created successfully",
                        productService.createProduct(request, images)
                ));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PUT /products/{id} — ADMIN only, multipart/form-data
    // ──────────────────────────────────────────────────────────────────────────
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update product",
            description = """
            Replaces all editable fields of an existing product.
            
            ## Image handling
            
            Images are **replaced** only when new files are provided.
            Omitting the `images` part leaves existing images unchanged.
            
            | Scenario | Result |
            |----------|--------|
            | `images` provided | Existing images deleted, new ones uploaded to Cloudinary |
            | `images` omitted | Existing images preserved |
            
            ## Slug regeneration
            
            Updating `name` always regenerates the slug.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product updated",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "product-updated",
                            value = """
                                    {
                                      "success": true,
                                      "message": "Product updated successfully",
                                      "data": {
                                        "id": "550e8400-e29b-41d4-a716-446655440000",
                                        "name": "Handmade Ceramic Mug — Large",
                                        "slug": "handmade-ceramic-mug-large",
                                        "price": 59.99,
                                        "stockType": "AVAILABLE",
                                        "stockQuantity": 8,
                                        "isActive": true,
                                        "isCustomOrder": false,
                                        "images": []
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Not authenticated",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                    "success": false,
                                    "message": "Unauthorized"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Authenticated but does not have ADMIN privileges",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                    "success": false,
                                    "message": "Access Denied"
                                    }
                                    """
                    )
            )
    )

    @ApiResponse(
            responseCode = "404",
            description = "Product or category not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "message": "Product not found"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "500",
            description = "Image upload to Cloudinary failed",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "message": "Image upload failed. Please try again later."
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<AppResponse<ProductResponse>> updateProduct(
            @Parameter(
                    description = "Product UUID",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id,
            @Parameter(description = "Updated product data as JSON", required = true)
            @RequestPart("data") @Valid ProductRequest request,
            @Parameter(description = "Replacement image files. Omit to keep existing images.")
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return ResponseEntity.ok(
                AppResponse.success(
                        "Product updated successfully",
                        productService.updateProduct(id, request, images)
                )
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DELETE /products/{id} — ADMIN only, soft delete
    // ──────────────────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete product",
            description = """
            Soft-deletes a product by setting its `deletedAt` timestamp.
            The product is no longer returned by `GET /products` or `GET /products/{slug}`.
            This operation is not reversible via the API.
            """
    )
    @ApiResponse(
            responseCode = "204",
            description = "Product deleted"
    )
    @ApiResponse(
            responseCode = "401",
            description = "Not authenticated",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                    "success": false,
                                    "message": "Unauthorized"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Authenticated but does not have ADMIN privileges",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                    "success": false,
                                    "message": "Access Denied"
                                    }
                                    """
                    )
            )
    )

    @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "message": "Product not found"
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<Void> deleteProduct(
            @Parameter(
                    description = "Product UUID",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id
    ) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PATCH /products/{id}/toggle — ADMIN only
    // ──────────────────────────────────────────────────────────────────────────
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Toggle product visibility",
            description = """
            Flips the `isActive` flag of a product.
            
            - `isActive: true` → product is visible in the catalog
            - `isActive: false` → product is hidden from buyers but not deleted
            
            Returns the product with its updated state.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Visibility toggled",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "deactivated",
                                    summary = "Product hidden from catalog",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "Product visibility updated",
                                              "data": {
                                                "id": "550e8400-e29b-41d4-a716-446655440000",
                                                "name": "Handmade Ceramic Mug",
                                                "slug": "handmade-ceramic-mug",
                                                "isActive": false
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "activated",
                                    summary = "Product visible in catalog",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "Product visibility updated",
                                              "data": {
                                                "id": "550e8400-e29b-41d4-a716-446655440000",
                                                "name": "Handmade Ceramic Mug",
                                                "slug": "handmade-ceramic-mug",
                                                "isActive": true
                                              }
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Not authenticated",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                    "success": false,
                                    "message": "Unauthorized"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Authenticated but does not have ADMIN privileges",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                    "success": false,
                                    "message": "Access Denied"
                                    }
                                    """
                    )
            )
    )

    @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "message": "Product not found"
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<AppResponse<ProductResponse>> toggleActive(
            @Parameter(
                    description = "Product UUID",
                    required = true,
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
}