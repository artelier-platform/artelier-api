package com.artelier.api.controller;

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
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller exposing the public product catalog.
 *
 * <p>All endpoints are mounted under {@code /products} and are <b>public</b>:
 * they do not require a Bearer token. They are explicitly excluded from the
 * global {@code bearerAuth} security requirement through
 * {@link SecurityRequirements}, so Swagger UI does not send an
 * {@code Authorization} header for them.</p>
 *
 * <p>This controller only exposes read operations.</p>
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
 * documentation). Filtering, caching and lookup logic live in
 * {@link ProductService}.</p>
 */
@AllArgsConstructor
@RestController
@RequestMapping("/products")
@Tag(
        name = "Products",
        description = """
        Product catalog management for the Artelier marketplace.

        ## Access Control

        All endpoints in this group are **public**. No authentication is required
        and no `Authorization` header must be sent.

        | Endpoint | Access |
        |----------|--------|
        | `GET /products` | Public |
        | `GET /products/{slug}` | Public |

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

    // =========================================================
    // OpenAPI shared descriptions
    // =========================================================

    private static final String DESC_PRODUCT_NOT_FOUND = "Product not found";
    private static final String DESC_SERVER_ERROR = "Unexpected server error";

    // =========================================================
    // Dependencies
    // =========================================================

    private final ProductService productService;

    // =========================================================
    // LIST PRODUCTS
    // =========================================================

    /**
     * Retrieves a paginated list of active products, optionally filtered by
     * category.
     *
     * @param categoryId optional category identifier; when omitted, products of
     *                   every category are returned
     * @param pageable   pagination and sorting parameters
     * @return {@code 200} with a page of products
     */
    @GetMapping
    @SecurityRequirements
    @Operation(
            operationId = "getAllProducts",
            summary = "List products",
            description = """
            Returns a paginated list of active products, optionally filtered by category.

            ## Filtering

            The optional `categoryId` parameter restricts the result to a single category.

            ## Pagination

            Pagination and sorting are controlled with the standard Spring Data
            parameters: `page`, `size` and `sort`.

            ## Caching

            Results are cached by `categoryId + page + size + sort`.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Products retrieved",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.ProductPageResponseBody.class),
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
                                              "slug": "ceramics"
                                            },
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
                                        "totalElements": 1,
                                        "totalPages": 1,
                                        "first": true,
                                        "last": true,
                                        "numberOfElements": 1,
                                        "empty": false
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid query parameter (for example, a malformed category UUID)",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<AppResponse<Page<ProductResponse>>> getAllProducts(
            @Parameter(
                    description = "Filter by category UUID. Optional — omit to return all categories.",
                    example = "660e8400-e29b-41d4-a716-446655440001"
            )
            @RequestParam(required = false) UUID categoryId,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(AppResponse.success(
                "Products retrieved",
                productService.getAllProducts(categoryId, pageable)
        ));
    }

    // =========================================================
    // GET BY SLUG
    // =========================================================

    /**
     * Retrieves a single product by its URL-friendly slug.
     *
     * @param slug URL-friendly product identifier
     * @return {@code 200} with the requested product
     */
    @GetMapping("/{slug}")
    @SecurityRequirements
    @Operation(
            operationId = "getProductBySlug",
            summary = "Get product by slug",
            description = """
            Returns a single product identified by its URL-friendly slug.

            Example: `"Handmade Ceramic Mug"` → `"handmade-ceramic-mug"`.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.ProductResponseBody.class),
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
                                          "slug": "ceramics"
                                        },
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
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = DESC_PRODUCT_NOT_FOUND,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "product-not-found",
                            value = """
                                    {
                                      "success": false,
                                      "message": "Product not found"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<AppResponse<ProductResponse>> getBySlug(
            @Parameter(
                    description = "URL-friendly product identifier",
                    required = true,
                    example = "handmade-ceramic-mug"
            )
            @PathVariable String slug
    ) {
        return ResponseEntity.ok(AppResponse.success("Product retrieved", productService.getBySlug(slug)));
    }
}