package com.artelier.api.controller;

import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.StatsResponse;
import com.artelier.api.dto.response.swagger.SwaggerResponses;
import com.artelier.api.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the aggregated metrics of the administrative
 * dashboard.
 *
 * <p>All endpoints are mounted under {@code /admin/stats} and are restricted to
 * users with the {@code ADMIN} role through a class-level {@link PreAuthorize}.
 * The class carries the {@code bearerAuth} security requirement, so Swagger UI
 * sends the {@code Authorization} header for every operation.</p>
 *
 * <h2>Response conventions</h2>
 * <ul>
 *   <li>Every response is wrapped in {@link AppResponse}
 *       ({@code success}, {@code message}, optional {@code data}).</li>
 *   <li>Errors are translated to {@link AppResponse} with {@code success = false}
 *       by the global exception handler.</li>
 * </ul>
 *
 * <p>This class only handles HTTP concerns (routing, authorization and
 * documentation). The calculation of every metric lives in
 * {@link StatsService}.</p>
 */
@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Statistics",
        description = """
        Administrative dashboard metrics.

        Provides aggregated business indicators used by the admin dashboard.

        ## Metrics Included

        | Field | Description |
        |-------|-------------|
        | `totalSalesThisMonth` | Sum of all paid sales generated during the current UTC month |
        | `pendingOrders` | Number of orders waiting for payment |
        | `activeProducts` | Number of products currently visible in the catalog |
        | `topSellingProduct` | Product with the highest quantity sold across all orders |

        ## Access Control

        Only users with the `ADMIN` role can access this endpoint.
        """
)
public class StatsController {

    // =========================================================
    // OpenAPI shared descriptions
    // =========================================================

    private static final String DESC_UNAUTHORIZED = "Not authenticated";
    private static final String DESC_FORBIDDEN = "Access denied — ADMIN role required";
    private static final String DESC_SERVER_ERROR = "Unexpected server error";

    // =========================================================
    // Dependencies
    // =========================================================

    private final StatsService statsService;

    // =========================================================
    // DASHBOARD STATISTICS
    // =========================================================

    /**
     * Retrieves the metrics displayed in the administrative dashboard.
     *
     * @return {@code 200} with the dashboard statistics; {@code topSellingProduct}
     *         is {@code null} when no product has been sold yet
     */
    @GetMapping
    @Operation(
            operationId = "getDashboardStats",
            summary = "Get dashboard statistics",
            description = """
            Returns the main metrics displayed in the administrative dashboard.

            ## Access

            Restricted to users with the `ADMIN` role.

            ## Sales Calculation

            `totalSalesThisMonth` includes all paid sales generated since the first day
            of the current UTC month.

            ## Pending Orders

            Counts orders currently in the `PENDING_PAYMENT` status.

            ## Active Products

            Counts products where `isActive = true`.

            ## Top Selling Product

            Returns the product with the highest accumulated quantity sold.

            If no products have been sold yet, `topSellingProduct` is returned as `null`.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Statistics retrieved successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.StatsResponseBody.class),
                    examples = {
                            @ExampleObject(
                                    name = "dashboard-stats",
                                    summary = "Dashboard statistics available",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "Statistics retrieved",
                                              "data": {
                                                "totalSalesThisMonth": 12450.75,
                                                "pendingOrders": 8,
                                                "activeProducts": 42,
                                                "topSellingProduct": {
                                                  "id": "550e8400-e29b-41d4-a716-446655440000",
                                                  "name": "Handmade Ceramic Mug",
                                                  "totalSold": 125
                                                }
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "no-sales-yet",
                                    summary = "No products sold yet",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "Statistics retrieved",
                                              "data": {
                                                "totalSalesThisMonth": 0,
                                                "pendingOrders": 0,
                                                "activeProducts": 12,
                                                "topSellingProduct": null
                                              }
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = DESC_UNAUTHORIZED,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "unauthorized",
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
            description = DESC_FORBIDDEN,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "forbidden",
                            value = """
                                    {
                                      "success": false,
                                      "message": "Access Denied"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<AppResponse<StatsResponse>> getStats() {
        return ResponseEntity.ok(AppResponse.success("Statistics retrieved", statsService.getStats()));
    }
}