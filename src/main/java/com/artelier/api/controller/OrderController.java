package com.artelier.api.controller;

import com.artelier.api.dto.request.OrderRequest;
import com.artelier.api.dto.response.ApiResponse;
import com.artelier.api.dto.response.OrderResponse;
import com.artelier.api.entity.enums.OrderStatus;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/orders")
@Tag(
        name = "Orders",
        description = "Order management and purchase flow"
)
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;
    @Operation(
            summary = "Create order",
            description = "Creates a new order from the user's cart and sets it as PENDING_PAYMENT"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(

            @Parameter(hidden = true)
            @RequestHeader("Authorization") String authHeader,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Order payload",
                    required = true,
                    content = @Content(schema = @Schema(implementation = OrderRequest.class))
            )
            @Valid
            @RequestBody
            OrderRequest request
    ) {

        String token = extractToken(authHeader);
        String userEmail = jwtUtil.extractUsername(token);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Order created successfully",
                        orderService.createOrder(request, userEmail)
                )
        );
    }

    @Operation(
            summary = "Get my orders",
            description = "Returns the authenticated user's order history"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(

            @Parameter(hidden = true)
            @RequestHeader("Authorization") String authHeader
    ) {

        String token = extractToken(authHeader);
        String userEmail = jwtUtil.extractUsername(token);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Orders fetched",
                        orderService.getMyOrders(userEmail)
                )
        );
    }

    @Operation(
            summary = "Get all orders",
            description = "Returns paginated list of all orders (ADMIN only)"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(

            @Parameter(
                    description = "Filter by order status",
                    example = "PENDING_PAYMENT"
            )
            @RequestParam(required = false) OrderStatus status,

            Pageable pageable
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Orders fetched",
                        orderService.getAllOrders(status, pageable)
                )
        );
    }

    @Operation(
            summary = "Update order status",
            description = "Updates the status of an order (ADMIN only)"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(

            @Parameter(
                    description = "Order UUID",
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id,

            @Parameter(
                    description = "New order status",
                    example = "SHIPPED"
            )
            @RequestParam OrderStatus status
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Order status updated",
                        orderService.updateOrderStatus(id, status)
                )
        );
    }

    @Operation(
            summary = "Get order by ID",
            description = "Returns a specific order"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order fetched"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(

            @Parameter(
                    description = "Order UUID",
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Order fetched",
                        orderService.getOrderById(id)
                )
        );
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid Authorization header");
        }
        return authHeader.substring(7);
    }
}