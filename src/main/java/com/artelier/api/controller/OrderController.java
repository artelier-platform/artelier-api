package com.artelier.api.controller;

import com.artelier.api.dto.request.OrderRequest;
import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.OrderResponse;
import com.artelier.api.dto.response.swagger.SwaggerResponses;
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

/**
 * REST controller exposing order management and the purchase flow.
 *
 * <p>All endpoints are mounted under {@code /orders} and <b>require
 * authentication</b>: the class carries the {@code bearerAuth} security
 * requirement, so Swagger UI sends the {@code Authorization} header for every
 * operation.</p>
 *
 * <h2>Role-based behavior</h2>
 * <ul>
 *   <li>{@code BUYER}: creates orders, sees only their own orders and may
 *       cancel their own order while it is {@code PENDING_PAYMENT}.</li>
 *   <li>{@code ADMIN}: sees every order and may ship orders that are
 *       {@code PAID}.</li>
 * </ul>
 *
 * <h2>Order lifecycle</h2>
 * <p>The transitions triggered by the payment flow ({@code PROCESSING},
 * {@code PAID}) are performed by the system, not through this controller.
 * The only manual transitions exposed here are {@code PAID → SHIPPED} (admin)
 * and {@code PENDING_PAYMENT → CANCELLED} (buyer).</p>
 *
 * <h2>Response conventions</h2>
 * <ul>
 *   <li>Every response is wrapped in {@link AppResponse}
 *       ({@code success}, {@code message}, optional {@code data}).</li>
 *   <li>Errors are translated to {@link AppResponse} with {@code success = false}
 *       by the global exception handler.</li>
 * </ul>
 *
 * <p>This class only handles HTTP concerns (routing, validation, status codes
 * and documentation). Stock validation, pricing, ownership checks and status
 * transition rules live in {@link OrderService}.</p>
 */
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
        |------------|--------------|-----|
        | → `PENDING_PAYMENT` | Order created | Buyer |
        | `PENDING_PAYMENT` → `PROCESSING` | Payment initiated via Wompi | System |
        | `PROCESSING` → `PAID` | Wompi webhook: `APPROVED` | System |
        | `PROCESSING` → `PENDING_PAYMENT` | Wompi webhook: `DECLINED` / `VOIDED` | System |
        | `PAID` → `SHIPPED` | Manual status update | Admin only |
        | `PENDING_PAYMENT` → `CANCELLED` | Manual status update | Buyer only (own order) |

        ## Role-based Behavior

        | Endpoint | ADMIN | BUYER |
        |----------|-------|-------|
        | `POST /orders` | — | Creates a new order |
        | `GET /orders/my` | Own orders | Own orders |
        | `GET /orders` | **All** orders in the system | Only their own orders |
        | `GET /orders/{id}` | Any order | Own orders only |
        | `PATCH /orders/{id}/status` | `PAID → SHIPPED` | `PENDING_PAYMENT → CANCELLED` (own order) |

        ## Authentication

        All endpoints require a valid JWT access token:

           Authorization: Bearer <token>
        """
)
public class OrderController {

    // =========================================================
    // OpenAPI shared descriptions
    // =========================================================

    private static final String DESC_UNAUTHORIZED = "Not authenticated";
    private static final String DESC_ORDER_NOT_FOUND = "Order not found";
    private static final String DESC_SERVER_ERROR = "Unexpected server error";

    // =========================================================
    // OpenAPI shared examples
    // =========================================================

    private static final String EXAMPLE_UNAUTHORIZED = """
            {
              "success": false,
              "message": "Unauthorized"
            }
            """;

    private static final String EXAMPLE_ACCESS_DENIED = """
            {
              "success": false,
              "message": "Access denied to this order"
            }
            """;

    private static final String EXAMPLE_ORDER_NOT_FOUND = """
            {
              "success": false,
              "message": "Order not found"
            }
            """;

    // =========================================================
    // Dependencies
    // =========================================================

    private final OrderService orderService;

    // =========================================================
    // CREATE ORDER
    // =========================================================

    /**
     * Creates a new order for the authenticated user.
     *
     * @param principal authenticated user creating the order
     * @param request   items, shipping address and optional notes
     * @return {@code 200} with the created order in {@code PENDING_PAYMENT} status
     */
    @PostMapping
    @Operation(
            operationId = "createOrder",
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
                    schema = @Schema(implementation = SwaggerResponses.OrderResponseBody.class),
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
                                            "productName": "Handmade Ceramic Bowl",
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
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = DESC_UNAUTHORIZED,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "unauthorized", value = EXAMPLE_UNAUTHORIZED)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<AppResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OrderRequest request
    ) {
        return ResponseEntity.ok(AppResponse.success(
                "Order created successfully",
                orderService.createOrder(request, principal.getUsername())
        ));
    }

    // =========================================================
    // MY ORDERS
    // =========================================================

    /**
     * Retrieves the full order history of the authenticated user.
     *
     * @param principal authenticated user
     * @return {@code 200} with the user's orders, most recent first
     */
    @GetMapping("/my")
    @Operation(
            operationId = "getMyOrders",
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
                    schema = @Schema(implementation = SwaggerResponses.OrderListResponseBody.class),
                    examples = @ExampleObject(
                            name = "with-orders",
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
                                          "items": []
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = DESC_UNAUTHORIZED,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "unauthorized", value = EXAMPLE_UNAUTHORIZED)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<AppResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(AppResponse.success(
                "Orders fetched",
                orderService.getMyOrders(principal.getUsername())
        ));
    }

    // =========================================================
    // LIST ORDERS
    // =========================================================

    /**
     * Retrieves a paginated list of orders, filtered by the caller's role.
     *
     * @param status    optional status filter; when omitted, all statuses are returned
     * @param pageable  pagination and sorting parameters
     * @param principal authenticated user
     * @return {@code 200} with a page of orders
     */
    @GetMapping
    @Operation(
            operationId = "listOrders",
            summary = "List orders",
            description = """
            Returns a paginated list of orders. The result set depends on the caller's role:

            | Role | Visible orders |
            |------|----------------|
            | `ADMIN` | All orders in the system |
            | `BUYER` | Only their own orders |

            ## Filtering

            The optional `status` parameter restricts the result to orders in that status.

            ## Pagination

            Pagination and sorting are controlled with the standard Spring Data
            parameters: `page`, `size` and `sort`.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Orders retrieved — content filtered by role",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.OrderPageResponseBody.class),
                    examples = @ExampleObject(
                            name = "paginated-orders",
                            value = """
                                    {
                                      "success": true,
                                      "message": "Orders fetched",
                                      "data": {
                                        "content": [],
                                        "totalElements": 42,
                                        "totalPages": 5,
                                        "first": true,
                                        "last": false,
                                        "numberOfElements": 10,
                                        "empty": false
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid query parameter (for example, an unknown status value)",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = DESC_UNAUTHORIZED,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "unauthorized", value = EXAMPLE_UNAUTHORIZED)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
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
        return ResponseEntity.ok(AppResponse.success(
                "Orders fetched",
                orderService.getAllOrders(status, pageable, principal.getUser())
        ));
    }

    // =========================================================
    // UPDATE STATUS
    // =========================================================

    /**
     * Updates the status of an order according to the caller's role.
     *
     * @param id        order identifier
     * @param status    target status
     * @param principal authenticated user
     * @return {@code 200} with the updated order
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER')")
    @Operation(
            operationId = "updateOrderStatus",
            summary = "Update order status",
            description = """
            Updates the status of an order. Allowed transitions differ by role:

            | Role | Allowed transition | Precondition |
            |------|--------------------|--------------|
            | `ADMIN` | Any `PAID` → `SHIPPED` | Order must be in `PAID` status |
            | `BUYER` | Own order `PENDING_PAYMENT` → `CANCELLED` | Order must be in `PENDING_PAYMENT` status |

            Any other transition is rejected. Transitions driven by the payment flow
            are performed by the system and cannot be requested here.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Order status updated",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.OrderResponseBody.class),
                    examples = @ExampleObject(
                            name = "status-updated",
                            value = """
                                    {
                                      "success": true,
                                      "message": "Order status updated",
                                      "data": {
                                        "id": "770e8400-e29b-41d4-a716-446655440002",
                                        "status": "SHIPPED"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Transition not allowed or invalid parameter",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = DESC_UNAUTHORIZED,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "unauthorized", value = EXAMPLE_UNAUTHORIZED)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Buyer attempted to modify another user's order",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "access-denied", value = EXAMPLE_ACCESS_DENIED)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = DESC_ORDER_NOT_FOUND,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "order-not-found", value = EXAMPLE_ORDER_NOT_FOUND)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<AppResponse<OrderResponse>> updateStatus(
            @Parameter(
                    description = "Order UUID",
                    required = true,
                    example = "770e8400-e29b-41d4-a716-446655440002"
            )
            @PathVariable UUID id,
            @Parameter(
                    description = "Target status",
                    required = true,
                    schema = @Schema(implementation = OrderStatus.class)
            )
            @RequestParam OrderStatus status,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(AppResponse.success(
                "Order status updated",
                orderService.updateOrderStatus(id, status, principal.getUser())
        ));
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    /**
     * Retrieves a single order by its identifier.
     *
     * @param id        order identifier
     * @param principal authenticated user
     * @return {@code 200} with the requested order
     */
    @GetMapping("/{id}")
    @Operation(
            operationId = "getOrderById",
            summary = "Get order by ID",
            description = """
            Returns a specific order by its UUID.

            - `ADMIN` can retrieve any order.
            - `BUYER` can only retrieve their own orders.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Order found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.OrderResponseBody.class),
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
                                        "createdAt": "2024-05-07T00:00:00Z",
                                        "items": []
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = DESC_UNAUTHORIZED,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "unauthorized", value = EXAMPLE_UNAUTHORIZED)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Buyer attempted to access another user's order",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "access-denied", value = EXAMPLE_ACCESS_DENIED)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = DESC_ORDER_NOT_FOUND,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(name = "order-not-found", value = EXAMPLE_ORDER_NOT_FOUND)
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<AppResponse<OrderResponse>> getById(
            @Parameter(
                    description = "Order UUID",
                    required = true,
                    example = "770e8400-e29b-41d4-a716-446655440002"
            )
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(AppResponse.success(
                "Order fetched",
                orderService.getOrderById(id, principal.getUser())
        ));
    }
}