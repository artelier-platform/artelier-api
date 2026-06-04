package com.artelier.api.controller;

import com.artelier.api.dto.request.OrderRequest;
import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.OrderResponse;
import com.artelier.api.enums.OrderStatus;
import com.artelier.api.security.UserPrincipal;
import com.artelier.api.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/orders")
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Orders",
        description = """
        Order management and purchase flow.
        
        ## Order Status Flow
        
        | Transition | Triggered by | Who |
        |------------|-------------|-----|
        | → `PENDING_PAYMENT` | Order created | Buyer |
        | `PENDING_PAYMENT` → `PROCESSING` | Payment initiated via Wompi | System |
        | `PROCESSING` → `PAID` | Wompi webhook: APPROVED | System |
        | `PROCESSING` → `PENDING_PAYMENT` | Wompi webhook: DECLINED / VOIDED | System |
        | `PAID` → `SHIPPED` | Manual status update | Admin only |
        | `PENDING_PAYMENT` → `CANCELLED` | Manual status update | Buyer only (own order) |
        
        ## Role-based Behavior
        
        | Endpoint | ADMIN | BUYER |
        |----------|-------|-------|
        | `POST /orders` | — | Creates a new order |
        | `GET /orders/my` | Own orders | Own orders |
        | `GET /orders` | **All** orders in the system | Only their own orders |
        | `PATCH /{id}/status` | `PAID → SHIPPED` | `PENDING_PAYMENT → CANCELLED` (own order) |
        | `GET /{id}` | Any order | Own orders only |
        """
)
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(
            summary = "Create order",
            description = """
            Creates a new order from the provided items and sets it to `PENDING_PAYMENT`.
            
            ## Stock Validation
            
            For each item, stock is validated at creation time:
            - Products with `UNLIMITED` stock type skip validation.
            - Products with `AVAILABLE` or `MADE_TO_ORDER` stock must have enough units.
              If not, the entire request is rejected.
            
            ## Pricing
            
            `unitPrice` is captured at the moment the order is created.
            Price changes after order creation do not affect existing orders.
            
            ## Next Step
            
            After creating the order, call `POST /payments/orders/{orderId}` to initiate
            payment via the Wompi Checkout Widget.
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Items, shipping address, and optional notes",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = OrderRequest.class),
                            examples = @ExampleObject(
                                    name = "multi-item-order",
                                    summary = "Order with two products",
                                    value = """
                                            {
                                              "items": [
                                                {
                                                  "productId": "550e8400-e29b-41d4-a716-446655440000",
                                                  "quantity": 2,
                                                  "customNotes": "Gift wrap this one"
                                                },
                                                {
                                                  "productId": "660e8400-e29b-41d4-a716-446655440001",
                                                  "quantity": 1
                                                }
                                              ],
                                              "shippingAddress": "Cra 123 #45-67, Bogotá, Colombia",
                                              "notes": "Please deliver after 5 PM"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponse(
            responseCode = "200",
            description = "Order created — status is PENDING_PAYMENT",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "order-created",
                            value = """
                                    {
                                      "success": true,
                                      "message": "Order created successfully",
                                      "data": {
                                        "id": "770e8400-e29b-41d4-a716-446655440002",
                                        "status": "PENDING_PAYMENT",
                                        "subtotal": 89.97,
                                        "total": 89.97,
                                        "shippingAddress": "Cra 123 #45-67, Bogotá, Colombia",
                                        "notes": "Please deliver after 5 PM",
                                        "createdAt": "2024-05-07T00:00:00Z",
                                        "items": [
                                          {
                                            "productId": "550e8400-e29b-41d4-a716-446655440000",
                                            "productName": "Handmade Ceramic Mug",
                                            "quantity": 2,
                                            "unitPrice": 29.99,
                                            "customNotes": "Gift wrap this one"
                                          },
                                          {
                                            "productId": "660e8400-e29b-41d4-a716-446655440001",
                                            "productName": "Artisan Notebook",
                                            "quantity": 1,
                                            "unitPrice": 29.99
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
            description = "Validation failed or insufficient stock",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "validation-error",
                                    summary = "Missing required field",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "shippingAddress must not be blank"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "insufficient-stock",
                                    summary = "Not enough stock for a product",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "Insufficient stock for product: Handmade Ceramic Mug (available: 1)"
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
    public ResponseEntity<AppResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OrderRequest request
    ) {
        return ResponseEntity.ok(
                AppResponse.success(
                        "Order created successfully",
                        orderService.createOrder(request, principal.getUsername())
                )
        );
    }

    @GetMapping("/my")
    @Operation(
            summary = "Get my orders",
            description = """
            Returns the full order history of the authenticated user, sorted
            by creation date descending (most recent first).
            
            This endpoint is available to all roles. Each user sees only their own orders.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Order history retrieved",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "with-orders",
                                    summary = "User has orders",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "Orders fetched",
                                              "data": [
                                                {
                                                  "id": "770e8400-e29b-41d4-a716-446655440002",
                                                  "status": "PAID",
                                                  "subtotal": 89.97,
                                                  "total": 89.97,
                                                  "shippingAddress": "Cra 123 #45-67, Bogotá, Colombia",
                                                  "createdAt": "2024-05-07T00:00:00Z",
                                                  "items": [
                                                    {
                                                      "productId": "550e8400-e29b-41d4-a716-446655440000",
                                                      "productName": "Handmade Ceramic Mug",
                                                      "quantity": 2,
                                                      "unitPrice": 29.99
                                                    }
                                                  ]
                                                }
                                              ]
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "no-orders",
                                    summary = "User has no orders yet",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "Orders fetched",
                                              "data": []
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
    public ResponseEntity<AppResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                AppResponse.success(
                        "Orders fetched",
                        orderService.getMyOrders(principal.getUsername())
                )
        );
    }

    @GetMapping
    @Operation(
            summary = "List orders",
            description = """
            Returns a paginated list of orders. The result set depends on the caller's role:
            
            | Role | Visible orders |
            |------|----------------|
            | `ADMIN` | All orders in the system |
            | `BUYER` | Only their own orders |
            
            Both roles can filter by `status` and use standard Spring pagination
            parameters (`page`, `size`, `sort`).
            
            ## Pagination defaults
            
            | Parameter | Default |
            |-----------|---------|
            | `page` | `0` |
            | `size` | `20` |
            | `sort` | unsorted |
            
            ## Sort examples
            
            - `?sort=createdAt,desc` — newest first
            - `?sort=total,asc` — cheapest first
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Orders retrieved — content filtered by role",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "admin-all-orders",
                                    summary = "ADMIN — paginated view of all orders",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "Orders fetched",
                                              "data": {
                                                "content": [
                                                  {
                                                    "id": "770e8400-e29b-41d4-a716-446655440002",
                                                    "status": "SHIPPED",
                                                    "subtotal": 89.97,
                                                    "total": 89.97,
                                                    "shippingAddress": "Cra 123 #45-67, Bogotá, Colombia",
                                                    "createdAt": "2024-05-07T00:00:00Z",
                                                    "items": []
                                                  }
                                                ],
                                                "pageable": {
                                                  "pageNumber": 0,
                                                  "pageSize": 10,
                                                  "sort": { "sorted": false }
                                                },
                                                "totalElements": 42,
                                                "totalPages": 5,
                                                "first": true,
                                                "last": false,
                                                "numberOfElements": 10,
                                                "empty": false
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "buyer-own-orders",
                                    summary = "BUYER — paginated view of own orders only",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "Orders fetched",
                                              "data": {
                                                "content": [
                                                  {
                                                    "id": "880e8400-e29b-41d4-a716-446655440003",
                                                    "status": "PENDING_PAYMENT",
                                                    "subtotal": 29.99,
                                                    "total": 29.99,
                                                    "shippingAddress": "Calle 45 #12-34, Medellín, Colombia",
                                                    "createdAt": "2024-05-08T10:00:00Z",
                                                    "items": []
                                                  }
                                                ],
                                                "pageable": {
                                                  "pageNumber": 0,
                                                  "pageSize": 10,
                                                  "sort": { "sorted": false }
                                                },
                                                "totalElements": 3,
                                                "totalPages": 1,
                                                "first": true,
                                                "last": true,
                                                "numberOfElements": 3,
                                                "empty": false
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
    public ResponseEntity<AppResponse<Page<OrderResponse>>> getAllOrders(
            @Parameter(
                    description = "Filter by order status. Optional — omit to return all statuses.",
                    schema = @Schema(implementation = OrderStatus.class),
                    example = "PAID"
            )
            @RequestParam(required = false) OrderStatus status,
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                AppResponse.success(
                        "Orders fetched",
                        orderService.getAllOrders(status, pageable, principal.getUser())
                )
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER')")
    @Operation(
            summary = "Update order status",
            description = """
            Updates the status of an order. Allowed transitions differ by role:
            
            | Role | Allowed transition | Precondition |
            |------|--------------------|--------------|
            | `ADMIN` | Any `PAID` → `SHIPPED` | Order must be in `PAID` status |
            | `BUYER` | Own order `PENDING_PAYMENT` → `CANCELLED` | Order must be in `PENDING_PAYMENT` status |
            
            Any attempt to perform a transition outside the allowed rules
            is rejected with `HTTP 400`.
            
            A buyer attempting to update another user's order is rejected with `HTTP 403`.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Order status updated",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "admin-shipped",
                                    summary = "ADMIN — order shipped",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "Order status updated",
                                              "data": {
                                                "id": "770e8400-e29b-41d4-a716-446655440002",
                                                "status": "SHIPPED",
                                                "subtotal": 89.97,
                                                "total": 89.97,
                                                "shippingAddress": "Cra 123 #45-67, Bogotá, Colombia",
                                                "createdAt": "2024-05-07T00:00:00Z",
                                                "items": []
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "buyer-cancelled",
                                    summary = "BUYER — order cancelled",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "Order status updated",
                                              "data": {
                                                "id": "880e8400-e29b-41d4-a716-446655440003",
                                                "status": "CANCELLED",
                                                "subtotal": 29.99,
                                                "total": 29.99,
                                                "shippingAddress": "Calle 45 #12-34, Medellín, Colombia",
                                                "createdAt": "2024-05-08T10:00:00Z",
                                                "items": []
                                              }
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Transition not allowed for the current status or role",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "admin-invalid-transition",
                                    summary = "Admin tried to set a status other than SHIPPED",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "Admin can only transition orders to SHIPPED"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "order-not-paid",
                                    summary = "Admin tried to ship an order that isn't PAID",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "Order must be in PAID status to be shipped, current status: PROCESSING"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "buyer-invalid-transition",
                                    summary = "Buyer tried to set a status other than CANCELLED",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "You can only cancel your order"
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Buyer attempted to modify another user's order",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "message": "Access denied to this order"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Order not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "message": "Order not found"
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<AppResponse<OrderResponse>> updateStatus(
            @Parameter(
                    description = "Order UUID",
                    required = true,
                    example = "770e8400-e29b-41d4-a716-446655440002"
            )
            @PathVariable UUID id,
            @Parameter(
                    description = """
                    Target status. Allowed values depend on the caller's role:
                    - `ADMIN` → `SHIPPED`
                    - `BUYER` → `CANCELLED`
                    """,
                    required = true,
                    schema = @Schema(implementation = OrderStatus.class)
            )
            @RequestParam OrderStatus status,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                AppResponse.success(
                        "Order status updated",
                        orderService.updateOrderStatus(id, status, principal.getUser())
                )
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get order by ID",
            description = """
            Returns a specific order by its UUID.
            
            - `ADMIN` can retrieve any order.
            - `BUYER` can only retrieve their own orders. Attempting to access
              another user's order returns `HTTP 403`.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Order found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "order-detail",
                            value = """
                                    {
                                      "success": true,
                                      "message": "Order fetched",
                                      "data": {
                                        "id": "770e8400-e29b-41d4-a716-446655440002",
                                        "status": "PAID",
                                        "subtotal": 89.97,
                                        "total": 89.97,
                                        "shippingAddress": "Cra 123 #45-67, Bogotá, Colombia",
                                        "notes": "Please deliver after 5 PM",
                                        "createdAt": "2024-05-07T00:00:00Z",
                                        "items": [
                                          {
                                            "productId": "550e8400-e29b-41d4-a716-446655440000",
                                            "productName": "Handmade Ceramic Mug",
                                            "quantity": 2,
                                            "unitPrice": 29.99,
                                            "customNotes": "Gift wrap this one"
                                          },
                                          {
                                            "productId": "660e8400-e29b-41d4-a716-446655440001",
                                            "productName": "Artisan Notebook",
                                            "quantity": 1,
                                            "unitPrice": 29.99
                                          }
                                        ]
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Buyer attempted to access another user's order",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "message": "Access denied to this order"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Order not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "message": "Order not found"
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<AppResponse<OrderResponse>> getById(
            @Parameter(
                    description = "Order UUID",
                    required = true,
                    example = "770e8400-e29b-41d4-a716-446655440002"
            )
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                AppResponse.success(
                        "Order fetched",
                        orderService.getOrderById(id, principal.getUser())
                )
        );
    }
}