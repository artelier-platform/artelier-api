package com.artelier.api.controller;

import com.artelier.api.dto.request.ProductRequest;
import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.ProductResponse;
import com.artelier.api.dto.response.swagger.SwaggerResponses;
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

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "List products", description = """
            Returns a paginated list of active products, optionally filtered by category.
            Results are cached by `categoryId + page + size + sort`.
            """)
    @ApiResponse(
            responseCode = "200",
            description = "Products retrieved",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.ProductPageResponseBody.class),
                    examples = @ExampleObject(name = "product-page", value = """
                            {
                              "success": true,
                              "message": "Products retrieved",
                              "data": {
                                "content": [
                                  {
                                    "id": "550e8400-e29b-41d4-a716-446655440000",
                                    "category": { "id": "550e8400-e29b-41d4-a716-446655440111", "name": "Ceramics", "slug": "ceramics" },
                                    "name": "Handmade Ceramic Mug",
                                    "slug": "handmade-ceramic-mug",
                                    "price": 49.99,
                                    "stockType": "AVAILABLE",
                                    "stockQuantity": 10,
                                    "isCustomOrder": false,
                                    "isActive": true,
                                    "createdAt": "2026-01-15T10:30:00Z",
                                    "images": []
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
                            """)
            )
    )
    public ResponseEntity<AppResponse<Page<ProductResponse>>> getAllProducts(
            @Parameter(description = "Filter by category UUID.", example = "660e8400-e29b-41d4-a716-446655440001")
            @RequestParam(required = false) UUID categoryId,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(AppResponse.success(
                "Products retrieved",
                productService.getAllProducts(categoryId, pageable)
        ));
    }

    @GetMapping("/{slug}")
    @SecurityRequirements
    @Operation(summary = "Get product by slug", description = """
            Returns a single product identified by its URL-friendly slug.
            Example: `"Handmade Ceramic Mug"` → `"handmade-ceramic-mug"`.
            """)
    @ApiResponse(
            responseCode = "200",
            description = "Product found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.ProductResponseBody.class),
                    examples = @ExampleObject(name = "product-detail", value = """
                            {
                              "success": true,
                              "message": "Product retrieved",
                              "data": {
                                "id": "550e8400-e29b-41d4-a716-446655440000",
                                "name": "Handmade Ceramic Mug",
                                "slug": "handmade-ceramic-mug",
                                "price": 49.99,
                                "stockType": "AVAILABLE",
                                "isActive": true,
                                "images": []
                              }
                            }
                            """)
            )
    )
    @ApiResponse(responseCode = "404", description = "Product not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(value = """
                            {
                             "success": false,\s
                             "message": "Product not found"\s
                             }
                            \s""")))
    public ResponseEntity<AppResponse<ProductResponse>> getBySlug(
            @Parameter(description = "URL-friendly product identifier", example = "handmade-ceramic-mug")
            @PathVariable String slug
    ) {
        return ResponseEntity.ok(AppResponse.success("Product retrieved", productService.getBySlug(slug)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create product", description = """
            Creates a new product and uploads the provided images to Cloudinary.
            Uses `multipart/form-data` with two parts: `data` (JSON) and `images` (files).
            Files and metadata are matched by index: `images[0]` → `data.images[0]`.
            """)
    @ApiResponse(
            responseCode = "201",
            description = "Product created",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.ProductResponseBody.class),
                    examples = @ExampleObject(name = "product-created", value = """
                            {
                              "success": true,
                              "message": "Product created successfully",
                              "data": { "id": "550e8400-e29b-41d4-a716-446655440000", "name": "Handmade Ceramic Mug", "isActive": true }
                            }
                            """)
            )
    )
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class)))
    @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(value = """
                            { "success": false, "message": "Unauthorized" }""")))
    @ApiResponse(responseCode = "403", description = "ADMIN role required",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(value = """
                            { "success": false, "message": "Access Denied" }""")))
    @ApiResponse(responseCode = "404", description = "Category not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(value = """
                            { "success": false, "message": "Category not found" }""")))
    @ApiResponse(responseCode = "500", description = "Image upload to Cloudinary failed",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class)))
    public ResponseEntity<AppResponse<ProductResponse>> createProduct(
            @Parameter(description = "Product data as JSON", required = true)
            @RequestPart("data") @Valid ProductRequest request,
            @Parameter(description = "Product image files (coordinated by index with data.images)")
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AppResponse.success(
                        "Product created successfully",
                        productService.createProduct(request, images)
                ));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product", description = """
            Replaces all editable fields of an existing product.
            Images are replaced only when new files are provided. Omitting `images` preserves existing ones.
            Updating `name` always regenerates the slug.
            """)
    @ApiResponse(
            responseCode = "200",
            description = "Product updated",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.ProductResponseBody.class),
                    examples = @ExampleObject(name = "product-updated", value = """
                            {
                              "success": true,
                              "message": "Product updated successfully",
                              "data": { "id": "550e8400-e29b-41d4-a716-446655440000", "name": "Handmade Ceramic Mug — Large" }
                            }
                            """)
            )
    )
    @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(value = """
                            { "success": false, "message": "Unauthorized" }""")))
    @ApiResponse(responseCode = "403", description = "ADMIN role required",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(value = """
                            { "success": false, "message": "Access Denied" }""")))
    @ApiResponse(responseCode = "404", description = "Product or category not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(value = """
                            { "success": false, "message": "Product not found" }""")))
    @ApiResponse(responseCode = "500", description = "Image upload to Cloudinary failed",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class)))
    public ResponseEntity<AppResponse<ProductResponse>> updateProduct(
            @Parameter(description = "Product UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Parameter(description = "Updated product data as JSON", required = true)
            @RequestPart("data") @Valid ProductRequest request,
            @Parameter(description = "Replacement image files. Omit to keep existing images.")
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return ResponseEntity.ok(AppResponse.success(
                "Product updated successfully",
                productService.updateProduct(id, request, images)
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete product", description = """
            Soft-deletes a product by setting its `deletedAt` timestamp.
            The product is no longer returned by catalog endpoints.
            This operation is not reversible via the API.
            """)
    @ApiResponse(responseCode = "204", description = "Product deleted")
    @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(value = """
                            { "success": false, "message": "Unauthorized" }""")))
    @ApiResponse(responseCode = "403", description = "ADMIN role required",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(value = """
                            { "success": false, "message": "Access Denied" }""")))
    @ApiResponse(responseCode = "404", description = "Product not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(value = """
                            { "success": false, "message": "Product not found" }""")))
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id
    ) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle product visibility", description = """
            Flips the `isActive` flag of a product.
            - `isActive: true` → visible in catalog
            - `isActive: false` → hidden from buyers, not deleted
            """)
    @ApiResponse(
            responseCode = "200",
            description = "Visibility toggled",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.ProductResponseBody.class),
                    examples = @ExampleObject(name = "toggled", value = """
                            {
                              "success": true,
                              "message": "Product visibility updated",
                              "data": { "id": "550e8400-e29b-41d4-a716-446655440000", "name": "Handmade Ceramic Mug", "isActive": false }
                            }
                            """)
            )
    )
    @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(value = """
                            { "success": false, "message": "Unauthorized" }""")))
    @ApiResponse(responseCode = "403", description = "ADMIN role required",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(value = """
                            { "success": false, "message": "Access Denied" }""")))
    @ApiResponse(responseCode = "404", description = "Product not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(value = """
                            { "success": false, "message": "Product not found" }""")))
    public ResponseEntity<AppResponse<ProductResponse>> toggleActive(
            @Parameter(description = "Product UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(AppResponse.success(
                "Product visibility updated",
                productService.toggleActive(id)
        ));
    }
}