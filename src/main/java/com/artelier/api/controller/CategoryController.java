package com.artelier.api.controller;

import com.artelier.api.dto.request.CategoryRequest;
import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.CategoryResponse;
import com.artelier.api.dto.response.swagger.SwaggerResponses;
import com.artelier.api.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/categories")
@Tag(
        name = "Categories",
        description = """
        Product category management and catalog classification.
        
        ## Purpose
        
        Categories are used to organize products into logical groups
        and improve catalog navigation, filtering, and discovery.
        
        ## Public Access
        
        Category listings are publicly accessible and do not require authentication.
        
        ## Administration
        
        Creating categories requires:
        
        - Valid JWT authentication
        - `ADMIN` role
        
        ## Slugs
        
        Every category contains a unique `slug` value used as a
        URL-friendly identifier.
        
        Example:
        
        - Name: `Ceramics`
        - Slug: `ceramics`
        
        Slugs must be unique across the catalog.
        """
)
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(
            summary = "Get all categories",
            description = """
        Retrieves all categories currently available in the catalog.
        
        ## Usage
        
        Categories can be used to:
        
        - Populate navigation menus
        - Build category filters
        - Organize products in the storefront
        
        ## Ordering
        
        Categories are returned according to the repository default ordering.
        
        ## Authentication
        
        This endpoint is public and does not require authentication.
        """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Categories retrieved successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.CategoryListResponseBody.class),
                    examples = @ExampleObject(
                            name = "categories-list",
                            value = """
                                {
                                  "success": true,
                                  "message": "Categories fetched",
                                  "data": [
                                    {
                                      "id": "550e8400-e29b-41d4-a716-446655440000",
                                      "name": "Ceramics",
                                      "slug": "ceramics",
                                      "description": "Handmade ceramic products including mugs and bowls"
                                    },
                                    {
                                      "id": "660e8400-e29b-41d4-a716-446655440001",
                                      "name": "Jewelry",
                                      "slug": "jewelry",
                                      "description": "Handcrafted necklaces, rings and bracelets"
                                    }
                                  ]
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "server-error",
                            value = """
                                {
                                  "success": false,
                                  "message": "An unexpected error occurred"
                                }
                                """
                    )
            )
    )
    @GetMapping
    public ResponseEntity<AppResponse<List<CategoryResponse>>> getAll() {
        return ResponseEntity.ok(AppResponse.success("Categories fetched", categoryService.getAll()));
    }

}