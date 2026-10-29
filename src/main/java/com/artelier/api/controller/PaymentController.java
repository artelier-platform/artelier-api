package com.artelier.api.controller;

import com.artelier.api.dto.request.PaymentRequest;
import com.artelier.api.dto.request.PaymentWebhookRequest;
import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.PaymentResponse;
import com.artelier.api.dto.response.swagger.SwaggerResponses;
import com.artelier.api.integration.wompi.dto.response.WompiFinancialInstitutionsResponse;
import com.artelier.api.integration.wompi.service.WompiSignatureValidator;
import com.artelier.api.security.UserPrincipal;
import com.artelier.api.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing the payment lifecycle, integrated with the Wompi
 * payment gateway.
 *
 * <p>All endpoints are mounted under {@code /payments}. Every endpoint requires
 * a Bearer token (global {@code bearerAuth} requirement) except
 * {@code POST /payments/webhook}, which is <b>public</b> and explicitly excluded
 * through {@link SecurityRequirements}: it is called by Wompi, which cannot send
 * a JWT, and is protected by signature validation instead.</p>
 *
 * <h2>Payment flow</h2>
 * <ol>
 *   <li>The client creates a pending payment for an order.</li>
 *   <li>The client opens the Wompi Checkout Widget using the returned
 *       {@code reference}.</li>
 *   <li>Wompi notifies the webhook with the transaction result.</li>
 *   <li>The payment status is updated and the client polls until a final
 *       state is reached.</li>
 * </ol>
 *
 * <h2>Response conventions</h2>
 * <ul>
 *   <li>Every JSON response is wrapped in {@link AppResponse}
 *       ({@code success}, {@code message}, optional {@code data}).</li>
 *   <li>The webhook is the exception: it returns an empty body, since Wompi
 *       only needs the HTTP status code.</li>
 *   <li>Errors are translated to {@link AppResponse} with {@code success = false}
 *       by the global exception handler.</li>
 * </ul>
 *
 * <p>This class only handles HTTP concerns (routing, validation, status codes
 * and documentation). Payment creation, confirmation and the Wompi calls live
 * in {@link PaymentService}; signature verification lives in
 * {@link WompiSignatureValidator}.</p>
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(
        name = "Payments",
        description = """
        Payment lifecycle management integrated with the Wompi payment gateway.

        ## Payment Flow

        1. Create a pending payment via `POST /payments/orders/{orderId}`
        2. Use the returned `reference` to open the Wompi Checkout Widget on the frontend
        3. Customer completes the payment inside the widget
        4. Wompi notifies `POST /payments/webhook` with the result
        5. Payment status is updated automatically
        6. Client polls `GET /payments/orders/{orderId}` until a final state is reached

        ## Supported Payment Methods

        | Method | Description |
        |--------|-------------|
        | `CARD` | Credit or debit card via Wompi widget |
        | `PSE` | Bank transfer — frontend must redirect the customer to `redirectUrl` |
        | `NEQUI` | Mobile wallet push notification |

        ## Payment Statuses

        | Status | Final | Description |
        |--------|-------|-------------|
        | `PENDING` | No | Payment created but not yet completed |
        | `APPROVED` | Yes | Payment successfully confirmed |
        | `DECLINED` | Yes | Payment rejected by Wompi or the financial institution |
        | `VOIDED` | Yes | Payment reversed or cancelled |
        | `ERROR` | Yes | Unexpected processing error |

        ## Authentication

        | Endpoint | Access |
        |----------|--------|
        | `POST /payments/webhook` | **Public** — secured by Wompi signature validation |
        | All other endpoints | Bearer token required |
        """
)
public class PaymentController {

    // =========================================================
    // Constants
    // =========================================================

    private static final String WOMPI_TRANSACTION_UPDATED_EVENT = "transaction.updated";
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    // =========================================================
    // OpenAPI shared descriptions
    // =========================================================

    private static final String DESC_UNAUTHORIZED = "Not authenticated";
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

    // =========================================================
    // Dependencies
    // =========================================================

    private final PaymentService paymentService;
    private final WompiSignatureValidator signatureValidator;

    // =========================================================
    // WEBHOOK
    // =========================================================

    /**
     * Receives transaction events sent by Wompi.
     *
     * <p>The signature is validated before anything else. Events other than
     * {@code transaction.updated} are acknowledged with {@code 200} and ignored,
     * so Wompi does not keep retrying them.</p>
     *
     * @param request webhook payload sent by Wompi
     * @return {@code 200} when the event was processed or ignored,
     *         {@code 401} when the signature is invalid
     */
    @PostMapping("/webhook")
    @SecurityRequirements
    @Operation(
            operationId = "receiveWompiWebhook",
            summary = "Receive Wompi webhook event",
            description = """
            Public endpoint used exclusively by Wompi to notify transaction status updates.
            Secured via SHA-256 signature validation instead of JWT.

            ## Processing

            1. The signature of the event is validated. Invalid signatures are rejected with `401`.
            2. Only `transaction.updated` events are processed.
               Any other event type is acknowledged with `200` and ignored.
            3. For `transaction.updated`, the payment status is updated accordingly.

            ## Response

            The response has no body: Wompi only needs the HTTP status code.

            ## Not for Client Use

            This endpoint is not meant to be called by the frontend.
            To know the result of a payment, poll `GET /payments/orders/{orderId}`.
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Event payload sent by Wompi",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaymentWebhookRequest.class)
                    )
            )
    )
    @ApiResponse(responseCode = "200", description = "Webhook event successfully received and processed (or ignored)")
    @ApiResponse(responseCode = "400", description = "Malformed webhook payload")
    @ApiResponse(responseCode = "401", description = "Invalid Wompi signature")
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<Void> handleWebhook(@RequestBody PaymentWebhookRequest request) {
        if (!signatureValidator.isValid(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!WOMPI_TRANSACTION_UPDATED_EVENT.equals(request.event())) {
            return ResponseEntity.ok().build();
        }
        paymentService.confirmPayment(request);
        return ResponseEntity.ok().build();
    }

    // =========================================================
    // CREATE PAYMENT
    // =========================================================

    /**
     * Creates a pending payment for an order.
     *
     * <p>The client IP is resolved from the request and forwarded to the
     * service, see {@link #extractClientIp(HttpServletRequest)}.</p>
     *
     * @param orderId     identifier of the order to be paid
     * @param request     payment data (method and method-specific fields)
     * @param httpRequest current HTTP request, used to resolve the client IP
     * @param principal   authenticated user; must own the order (or be an admin)
     * @return {@code 201} with the pending payment
     */
    @PostMapping("/orders/{orderId}")
    @Operation(
            operationId = "createPayment",
            summary = "Create pending payment for an order",
            description = """
            Creates a new pending payment associated with the specified order.

            This endpoint **must be called before** opening the Wompi Checkout Widget.
            The returned `reference` is required by Wompi to correlate the transaction.

            ## PSE — Redirect Required

            For PSE payments the response includes a `redirectUrl`.
            For `CARD` and `NEQUI`, `redirectUrl` is absent.

            ## Next Step

            Open the Wompi Checkout Widget with the returned `reference`, then poll
            `GET /payments/orders/{orderId}` until a final status is reached.
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payment method and method-specific data",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaymentRequest.class)
                    )
            )
    )
    @ApiResponse(
            responseCode = "201",
            description = "Pending payment created — ready to initiate Wompi Checkout",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.PaymentResponseBody.class),
                    examples = {
                            @ExampleObject(
                                    name = "card-or-nequi",
                                    summary = "CARD or NEQUI — no redirect",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "Payment created successfully",
                                              "data": {
                                                "id": "550e8400-e29b-41d4-a716-446655440000",
                                                "orderId": "660e8400-e29b-41d4-a716-446655440001",
                                                "reference": "ORDER-550e8400-1715000000000",
                                                "status": "PENDING",
                                                "amount": 120000.00
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "pse",
                                    summary = "PSE — redirect required",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "Payment created successfully",
                                              "data": {
                                                "id": "550e8400-e29b-41d4-a716-446655440000",
                                                "orderId": "660e8400-e29b-41d4-a716-446655440001",
                                                "reference": "ORDER-550e8400-1715000000000",
                                                "status": "PENDING",
                                                "amount": 120000.00,
                                                "redirectUrl": "https://bank.example.com/pse/redirect"
                                              }
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload",
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
    @ApiResponse(responseCode = "403", description = "The order belongs to another user")
    @ApiResponse(
            responseCode = "404",
            description = "Order not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "order-not-found",
                            value = """
                                    {
                                      "success": false,
                                      "message": "Order not found"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "500",
            description = DESC_SERVER_ERROR,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    public ResponseEntity<AppResponse<PaymentResponse>> createPayment(
            @Parameter(
                    name = "orderId",
                    description = "Unique identifier of the order to be paid",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID orderId,
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String clientIp = extractClientIp(httpRequest);
        PaymentResponse payment = paymentService.createPendingPayment(
                orderId, request, clientIp, principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AppResponse.success("Payment created successfully", payment));
    }

    // =========================================================
    // GET PAYMENT BY ORDER
    // =========================================================

    /**
     * Retrieves the payment record associated with an order.
     *
     * @param orderId   identifier of the order
     * @param principal authenticated user; must own the order (or be an admin)
     * @return {@code 200} with the current payment record
     */
    @GetMapping("/orders/{orderId}")
    @Operation(
            operationId = "getPaymentByOrder",
            summary = "Get payment status by order",
            description = """
            Retrieves the current payment record associated with the specified order.

            ## Polling Strategy

            Poll every **3 seconds** with a max timeout of **5 minutes** until
            a final state is reached: `APPROVED`, `DECLINED`, `ERROR`, or `VOIDED`.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Payment record found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.PaymentResponseBody.class),
                    examples = @ExampleObject(
                            name = "approved",
                            value = """
                                    {
                                      "success": true,
                                      "message": "Payment retrieved successfully",
                                      "data": {
                                        "id": "550e8400-e29b-41d4-a716-446655440000",
                                        "orderId": "660e8400-e29b-41d4-a716-446655440001",
                                        "wompiTransactionId": "1292-1602113476-10985",
                                        "reference": "ORDER-550e8400-1715000000000",
                                        "status": "APPROVED",
                                        "amount": 120000.00,
                                        "paymentMethod": "NEQUI",
                                        "paidAt": "2024-05-07T00:00:00Z"
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
    @ApiResponse(responseCode = "403", description = "The order belongs to another user")
    @ApiResponse(
            responseCode = "404",
            description = "No payment found for the specified order",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "payment-not-found",
                            value = """
                                    {
                                      "success": false,
                                      "message": "Payment not found for order"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(responseCode = "500", description = DESC_SERVER_ERROR)
    public ResponseEntity<AppResponse<PaymentResponse>> getPaymentByOrder(
            @Parameter(
                    name = "orderId",
                    description = "Unique identifier of the order",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(AppResponse.success(
                "Payment retrieved successfully",
                paymentService.findByOrderId(orderId, principal.getUser())
        ));
    }

    // =========================================================
    // FINANCIAL INSTITUTIONS
    // =========================================================

    /**
     * Retrieves the financial institutions currently supported by Wompi for PSE.
     *
     * @return {@code 200} with the list of institutions
     */
    @GetMapping("/financial-institutions")
    @Operation(
            operationId = "getFinancialInstitutions",
            summary = "List available PSE financial institutions",
            description = """
            Retrieves the list of financial institutions currently supported by Wompi for PSE.
            Call this before rendering the PSE payment form to populate the bank selector.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Financial institutions retrieved successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.FinancialInstitutionsResponseBody.class),
                    examples = @ExampleObject(
                            name = "institutions-list",
                            value = """
                                    {
                                      "success": true,
                                      "message": "Financial institutions retrieved successfully",
                                      "data": [
                                        { "financial_institution_code": "1007", "financial_institution_name": "Bancolombia" },
                                        { "financial_institution_code": "1022", "financial_institution_name": "Banco de Bogotá" }
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
    @ApiResponse(
            responseCode = "500",
            description = "Unable to retrieve financial institutions — Wompi error",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    public ResponseEntity<AppResponse<List<WompiFinancialInstitutionsResponse>>> getFinancialInstitutions() {
        return ResponseEntity.ok(AppResponse.success(
                "Financial institutions retrieved successfully",
                paymentService.getFinancialInstitutions()
        ));
    }

    // =========================================================
    // Helpers
    // =========================================================

    /**
     * Resolves the IP address of the client that made the request.
     *
     * <p>Uses the first address of the {@code X-Forwarded-For} header when
     * present (the original client behind a proxy or load balancer), and falls
     * back to {@link HttpServletRequest#getRemoteAddr()} otherwise.</p>
     *
     * <p><b>Note:</b> {@code X-Forwarded-For} is client-controlled unless it is
     * set or overwritten by a trusted proxy, so the result must not be used for
     * security decisions.</p>
     *
     * @param request current HTTP request
     * @return the resolved client IP address
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}