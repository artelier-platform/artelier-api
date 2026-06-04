package com.artelier.api.controller;

import com.artelier.api.dto.request.PaymentRequest;
import com.artelier.api.dto.request.PaymentWebhookRequest;
import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.PaymentResponse;
import com.artelier.api.integration.wompi.dto.response.WompiFinancialInstitutionsResponse;
import com.artelier.api.service.PaymentService;
import com.artelier.api.integration.wompi.service.WompiSignatureValidator;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
        
        > The webhook endpoint is intentionally public and secured through Wompi SHA-256 signature validation.
        """
)
public class PaymentController {

    private final PaymentService paymentService;
    private final WompiSignatureValidator signatureValidator;

    @PostMapping("/webhook")
    @SecurityRequirements
    @Operation(
            summary = "Receive Wompi webhook event",
            description = """
            Public endpoint used exclusively by Wompi to notify transaction status updates.
            
            Authentication via JWT is intentionally disabled for this endpoint.
            Requests are authenticated using Wompi SHA-256 signature validation.
            
            ## Event Processing
            
            | Event | Behavior |
            |-------|----------|
            | `transaction.updated` | Processed — internal payment status is updated |
            | Any other event | Acknowledged and silently ignored |
            
            ## Idempotency
            
            - Duplicate webhook events for the same transaction are safely ignored.
            - Payments already in a final state (`APPROVED`, `DECLINED`, `ERROR`, `VOIDED`)
              are not reprocessed.
            
            ## Security
            
            Any request with an invalid or missing Wompi signature is rejected with `HTTP 401`.
            The signature is validated by computing a SHA-256 checksum over the properties
            listed in `signature.properties` using the Wompi events secret.
            
            ## Retry Behavior
            
            If this endpoint returns a non-2xx response, Wompi will retry the delivery
            according to its internal retry policy. Ensure idempotency to avoid
            side effects on retries.
            
            > **Note:** This endpoint intentionally returns an empty body.
            > Wompi only reads the HTTP status code.
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Webhook payload sent by Wompi after a transaction state change",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaymentWebhookRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "transaction-approved",
                                            summary = "Transaction approved via NEQUI",
                                            value = """
                                                    {
                                                      "event": "transaction.updated",
                                                      "data": {
                                                        "transaction": {
                                                          "id": "1292-1602113476-10985",
                                                          "reference": "ORDER-550e8400-1715000000000",
                                                          "status": "APPROVED",
                                                          "payment_method_type": "NEQUI",
                                                          "amount_in_cents": 12000000
                                                        }
                                                      },
                                                      "signature": {
                                                        "checksum": "a3f5c9d8e1b2f4a6c8e0d2b4f6a8c0e2d4f6a8c0",
                                                        "properties": [
                                                          "transaction.id",
                                                          "transaction.status",
                                                          "transaction.amount_in_cents"
                                                        ]
                                                      },
                                                      "timestamp": 1715000000000
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "transaction-declined",
                                            summary = "Transaction declined via PSE",
                                            value = """
                                                    {
                                                      "event": "transaction.updated",
                                                      "data": {
                                                        "transaction": {
                                                          "id": "1292-1602113476-10986",
                                                          "reference": "ORDER-550e8400-1715000000001",
                                                          "status": "DECLINED",
                                                          "payment_method_type": "PSE",
                                                          "amount_in_cents": 5000000
                                                        }
                                                      },
                                                      "signature": {
                                                        "checksum": "b4e6d0f2a8c4e6d0f2a8c4e6d0f2a8c4e6d0f2a8",
                                                        "properties": [
                                                          "transaction.id",
                                                          "transaction.status",
                                                          "transaction.amount_in_cents"
                                                        ]
                                                      },
                                                      "timestamp": 1715000001000
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Webhook event successfully received and processed"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Malformed webhook payload — missing required fields or invalid JSON"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid Wompi signature — request rejected"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Unexpected server error during webhook processing"
    )
    public ResponseEntity<Void> handleWebhook(@RequestBody PaymentWebhookRequest request) {
        if (!signatureValidator.isValid(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!"transaction.updated".equals(request.getEvent())) {
            return ResponseEntity.ok().build();
        }

        paymentService.confirmPayment(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/orders/{orderId}")
    @Operation(
            summary = "Create pending payment for an order",
            description = """
            Creates a new pending payment associated with the specified order.
            
            This endpoint **must be called before** opening the Wompi Checkout Widget.
            The returned `reference` is required by Wompi to correlate the external
            transaction with the internal payment record.
            
            ## Idempotency
            
            If a `PENDING` payment already exists for the order, the existing record
            is returned as-is. A new payment is not created.
            
            ## PSE — Redirect Required
            
            For PSE payments the response includes a `redirectUrl`. The frontend **must**
            redirect the customer to this URL to complete the bank transfer on their
            financial institution's portal. After completion, Wompi notifies the webhook.
            
            For `CARD` and `NEQUI`, `redirectUrl` is absent (omitted by `@JsonInclude(NON_NULL)`).
            
            ## Initial Field Values
            
            | Field | Initial value | Populated when |
            |-------|---------------|----------------|
            | `status` | `PENDING` | — |
            | `wompiTransactionId` | absent | Wompi confirms the transaction |
            | `paymentMethod` | absent | Wompi confirms the transaction |
            | `redirectUrl` | absent / present | PSE only, at creation time |
            | `paidAt` | absent | Payment reaches `APPROVED` |
            """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Pending payment created — ready to initiate Wompi Checkout",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "card-or-nequi",
                                    summary = "CARD / NEQUI — no redirect required",
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
                                    summary = "PSE — redirectUrl present, frontend must redirect",
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
                                                "redirectUrl": "https://checkout.wompi.co/l/?data=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9..."
                                              }
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload — validation failed",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "validation-error",
                            summary = "Missing required payment_method",
                            value = """
                                    {
                                      "success": false,
                                      "message": "payment_method must not be null"
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
    public ResponseEntity<AppResponse<PaymentResponse>> createPayment(
            @Parameter(
                    name = "orderId",
                    description = "Unique identifier of the order to be paid",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID orderId,
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        PaymentResponse payment = paymentService.createPendingPayment(orderId, request, clientIp);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AppResponse.success("Payment created successfully", payment));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(
            summary = "Get payment status by order",
            description = """
            Retrieves the current payment record associated with the specified order.
            
            ## Polling Strategy
            
            After opening the Wompi Checkout Widget, poll this endpoint until the payment
            reaches a **final** state (`APPROVED`, `DECLINED`, `ERROR`, or `VOIDED`).
            
            Recommended: poll every **3 seconds** with a max timeout of **5 minutes**.
            
            ## Status Transitions
            
            ```
            PENDING ──► APPROVED
            PENDING ──► DECLINED
            PENDING ──► ERROR
            PENDING ──► VOIDED
            ```
            
            Final states do not transition further.
            
            ## Field Availability per Status
            
            | Field | PENDING | APPROVED | DECLINED / ERROR |
            |-------|---------|----------|-----------------|
            | `wompiTransactionId` | absent | present | present |
            | `paymentMethod` | absent | present | present |
            | `redirectUrl` | PSE only | absent | absent |
            | `paidAt` | absent | present | absent |
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Payment record found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "pending",
                                    summary = "PENDING — customer still in Wompi widget",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "Payment retrieved successfully",
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
                                    name = "approved",
                                    summary = "APPROVED — payment confirmed",
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
                            ),
                            @ExampleObject(
                                    name = "declined",
                                    summary = "DECLINED — rejected by financial institution",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "Payment retrieved successfully",
                                              "data": {
                                                "id": "550e8400-e29b-41d4-a716-446655440000",
                                                "orderId": "660e8400-e29b-41d4-a716-446655440001",
                                                "wompiTransactionId": "1292-1602113476-10986",
                                                "reference": "ORDER-550e8400-1715000000000",
                                                "status": "DECLINED",
                                                "amount": 120000.00,
                                                "paymentMethod": "PSE"
                                              }
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "No payment found for the specified order",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "not-found",
                            value = """
                                    {
                                      "success": false,
                                      "message": "Payment not found for order 660e8400-e29b-41d4-a716-446655440001"
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<AppResponse<PaymentResponse>> getPaymentByOrder(
            @Parameter(
                    name = "orderId",
                    description = "Unique identifier of the order whose payment is being queried",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(
                AppResponse.success("Payment retrieved successfully", paymentService.findByOrderId(orderId))
        );
    }

    @GetMapping("/financial-institutions")
    @Operation(
            summary = "List available PSE financial institutions",
            description = """
            Retrieves the list of financial institutions currently supported by Wompi
            for PSE transactions.
            
            ## Usage
            
            Call this endpoint before rendering the PSE payment form to populate
            the bank selector dropdown. Pass `financial_institution_code` as the
            institution identifier when submitting the PSE payment request.
            
            ## Caching
            
            The list reflects Wompi's current active institutions and may change
            without notice. Avoid caching this response for more than a few minutes.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Financial institutions retrieved successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "institutions-list",
                            summary = "Sample list of supported PSE institutions",
                            value = """
                                    {
                                      "success": true,
                                      "message": "Financial institutions retrieved successfully",
                                      "data": [
                                        {
                                          "financial_institution_code": "1007",
                                          "financial_institution_name": "Bancolombia"
                                        },
                                        {
                                          "financial_institution_code": "1022",
                                          "financial_institution_name": "Banco de Bogotá"
                                        },
                                        {
                                          "financial_institution_code": "1032",
                                          "financial_institution_name": "Banco Caja Social"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "500",
            description = "Unable to retrieve financial institutions — Wompi downstream error",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "server-error",
                            value = """
                                    {
                                      "success": false,
                                      "message": "Unable to retrieve financial institutions"
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<AppResponse<List<WompiFinancialInstitutionsResponse>>> getFinancialInstitutions() {
        return ResponseEntity.ok(
                AppResponse.success(
                        "Financial institutions retrieved successfully",
                        paymentService.getFinancialInstitutions()
                )
        );
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}