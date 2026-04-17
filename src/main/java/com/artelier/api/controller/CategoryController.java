package com.artelier.api.controller;

import com.artelier.api.dto.request.CategoryRequest;
import com.artelier.api.dto.response.ApiResponse;
import com.artelier.api.dto.response.CategoryResponse;
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
        description = "Category management and catalog classification"
)
public class CategoryController {

    private final CategoryService categoryService;


    @Operation(
            summary = "Get all categories",
            description = "Returns a list of all product categories"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Categories fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Categories fetched",
                        categoryService.getAll()
                )
        );
    }

    @Operation(
            summary = "Create category",
            description = "Creates a new category (ADMIN only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Category created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Category already exists")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Category data",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CategoryRequest.class))
            )
            @Valid
            @RequestBody CategoryRequest request
    ) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Category created",
                                categoryService.create(request)
                        )
                );
    }
}