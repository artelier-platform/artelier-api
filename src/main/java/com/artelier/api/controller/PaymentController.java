package com.artelier.api.controller;

import com.artelier.api.dto.request.PaymentWebhookRequest;
import com.artelier.api.dto.response.PaymentResponse;
import com.artelier.api.service.PaymentService;
import com.artelier.api.service.WompiSignatureValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(
        name = "Payments",
        description = """
        Manages payment lifecycle integrated with the Wompi payment gateway.
        
        **Flow:**
        1. Call `POST /payments/orders/{orderId}` to create a pending payment and get the `reference`.
        2. Open the Wompi widget on the frontend using `reference` + your Wompi public key.
        3. Wompi calls `POST /payments/webhook` automatically when the transaction status changes.
        4. Poll `GET /payments/orders/{orderId}` to check the final payment status.
        
        > ⚠️ The webhook endpoint must remain **public** (no JWT). Security is enforced via Wompi signature validation.
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
            Endpoint called automatically by Wompi when a transaction status changes.
            
            **Do not call this endpoint manually in production.**
            
            Every incoming event is validated against the Wompi signature (SHA-256 checksum)
            before any processing occurs. Events other than `transaction.updated` are acknowledged
            but ignored. Duplicate events for already-finalized payments are handled idempotently.
            
            **Wompi event types received:**
            - `transaction.updated` → processed (APPROVED, DECLINED, ERROR)
            - Others → acknowledged with 200, ignored
            
            **Wompi retry policy:** if this endpoint returns anything other than 2xx,
            Wompi will retry the webhook up to 3 times with exponential backoff.
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Wompi webhook event payload",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaymentWebhookRequest.class),
                            examples = @ExampleObject(
                                    name = "transaction.updated — APPROVED",
                                    summary = "Payment approved via Nequi",
                                    value = """
                        {
                          "event": "transaction.updated",
                          "data": {
                            "transaction": {
                              "id": "wompi_tx_98765",
                              "reference": "550e8400-e29b-41d4-a716-446655440000",
                              "status": "APPROVED",
                              "payment_method_type": "NEQUI",
                              "amount_in_cents": 12000000
                            }
                          },
                          "signature": {
                            "checksum": "a3f1c9e2b4d7...",
                            "properties": [
                              "transaction.id",
                              "transaction.status",
                              "transaction.amount_in_cents"
                            ]
                          },
                          "timestamp": 1715000000,
                          "environment": "production"
                        }
                        """
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Event received and processed successfully (or acknowledged and ignored)"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid Wompi signature — event rejected",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error — Wompi will retry",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
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
            Creates a `PENDING` payment record associated with the given order.
            
            Call this endpoint **before** opening the Wompi widget on the frontend.
            The `reference` field in the response must be passed to the Wompi widget
            so that the gateway can correlate the transaction with this payment record.
            
            **Idempotent:** if a payment already exists for the order, the existing record
            is returned instead of creating a duplicate.
            
            **Payment method** is set to `null` at creation — it is updated once Wompi
            confirms the transaction via webhook with the actual method used (NEQUI, PSE, CARD).
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Pending payment created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaymentResponse.class),
                            examples = @ExampleObject(
                                    name = "Pending payment",
                                    value = """
                        {
                          "id": "a1b2c3d4-0000-0000-0000-000000000001",
                          "orderId": "550e8400-e29b-41d4-a716-446655440000",
                          "wompiTransactionId": null,
                          "status": "PENDING",
                          "amount": 120000.00,
                          "paymentMethod": null,
                          "paidAt": null
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    public ResponseEntity<PaymentResponse> createPayment(
            @Parameter(
                    description = "UUID of the order to create the payment for",
                    example = "550e8400-e29b-41d4-a716-446655440000",
                    required = true
            )
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPendingPayment(orderId));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(
            summary = "Get payment status by order",
            description = """
            Returns the current payment record associated with the given order.
            
            Use this endpoint to **poll payment status** from the frontend after the user
            completes (or abandons) the Wompi widget flow.
            
            **Possible statuses:**
            | Status | Description |
            |--------|-------------|
            | `PENDING` | Payment created, user has not completed the flow yet |
            | `APPROVED` | Transaction confirmed by Wompi — order marked as PAID |
            | `DECLINED` | Transaction rejected by Wompi or the user's bank |
            | `ERROR` | Unexpected status received from Wompi |
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment record found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaymentResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "APPROVED",
                                            summary = "Payment approved",
                                            value = """
                            {
                              "id": "a1b2c3d4-0000-0000-0000-000000000001",
                              "orderId": "550e8400-e29b-41d4-a716-446655440000",
                              "wompiTransactionId": "wompi_tx_98765",
                              "status": "APPROVED",
                              "amount": 120000.00,
                              "paymentMethod": "NEQUI",
                              "paidAt": "2025-05-05T14:32:10Z"
                            }
                            """
                                    ),
                                    @ExampleObject(
                                            name = "DECLINED",
                                            summary = "Payment declined",
                                            value = """
                            {
                              "id": "a1b2c3d4-0000-0000-0000-000000000001",
                              "orderId": "550e8400-e29b-41d4-a716-446655440000",
                              "wompiTransactionId": "wompi_tx_98765",
                              "status": "DECLINED",
                              "amount": 120000.00,
                              "paymentMethod": "PSE",
                              "paidAt": null
                            }
                            """
                                    ),
                                    @ExampleObject(
                                            name = "PENDING",
                                            summary = "Payment still pending",
                                            value = """
                            {
                              "id": "a1b2c3d4-0000-0000-0000-000000000001",
                              "orderId": "550e8400-e29b-41d4-a716-446655440000",
                              "wompiTransactionId": null,
                              "status": "PENDING",
                              "amount": 120000.00,
                              "paymentMethod": null,
                              "paidAt": null
                            }
                            """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No payment found for the given order",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    public ResponseEntity<PaymentResponse> getPaymentByOrder(
            @Parameter(
                    description = "UUID of the order whose payment status is being queried",
                    example = "550e8400-e29b-41d4-a716-446655440000",
                    required = true
            )
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(paymentService.findByOrderId(orderId));
    }
}