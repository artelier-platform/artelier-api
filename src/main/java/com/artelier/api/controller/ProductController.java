package com.artelier.api.controller;

import com.artelier.api.dto.request.ProductRequest;
import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.ProductResponse;
import com.artelier.api.integration.cloudinary.service.CloudinaryService;
import com.artelier.api.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.*;
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
@Tag(
        name = "Products",
        description = "Product management and public catalog"
)
public class ProductController {

    private final ProductService productService;
    private final CloudinaryService cloudinaryService;



    @Operation(
            summary = "Get all products",
            description = "Returns paginated list of products. Supports filtering by category, name and price range."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<AppResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) UUID categoryId,
            Pageable pageable
    ) {

        Page<ProductResponse> data =
                productService.getAllProducts(categoryId, pageable);

        return ResponseEntity.ok(AppResponse.success("Products fetched", data));
    }


    @Operation(
            summary = "Get product by slug",
            description = "Returns a single product using its unique slug"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{slug}")
    public ResponseEntity<AppResponse<ProductResponse>> getBySlug(

            @Parameter(
                    description = "Unique product slug",
                    example = "handmade-ceramic-mug"
            )
            @PathVariable String slug
    ) {

        return ResponseEntity.ok(
                AppResponse.success("Product fetched", productService.getBySlug(slug))
        );
    }


    @Operation(
            summary = "Create product",
            description = "Creates a new product (ADMIN only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AppResponse<ProductResponse>> createProduct(
            @RequestPart("data") @Valid ProductRequest request,
            @RequestPart("images") List<MultipartFile> images
    ) {
        return ResponseEntity.ok(
                AppResponse.success("Product created",
                        productService.createProduct(request, images))
        );
    }

    @Operation(
            summary = "Update product",
            description = "Updates an existing product (ADMIN only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppResponse<ProductResponse>> update(

            @Parameter(
                    description = "Product UUID",
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id,

            @Valid
            @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(
                AppResponse.success("Product updated", productService.updateProduct(id, request))
        );
    }

    @Operation(
            summary = "Delete product",
            description = "Soft deletes a product (ADMIN only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppResponse<Void>> delete(

            @Parameter(
                    description = "Product UUID",
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id
    ) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(AppResponse.success("Product deleted"));
    }


    @Operation(
            summary = "Toggle product active state",
            description = "Enable or disable a product (ADMIN only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppResponse<ProductResponse>> toggleActive(

            @Parameter(
                    description = "Product UUID",
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                AppResponse.success("Product updated", productService.toggleActive(id))
        );
    }

    @Operation(
            summary = "Upload image",
            description = "Uploads an image to Cloudinary and returns metadata"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Image uploaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid file")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> upload(

            @Parameter(
                    description = "Image file",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
            )
            @RequestParam("file")
            MultipartFile file
    ) {

        return ResponseEntity.ok(cloudinaryService.upload(file));
    }
}