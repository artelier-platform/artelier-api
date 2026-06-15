package com.artelier.api.controller;

import com.artelier.api.dto.request.PaymentRequest;
import com.artelier.api.dto.request.PaymentWebhookRequest;
import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.PaymentResponse;
import com.artelier.api.dto.response.swagger.SwaggerResponses;
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
        """
)
public class PaymentController {

    private final PaymentService paymentService;
    private final WompiSignatureValidator signatureValidator;

    @PostMapping("/webhook")
    @SecurityRequirements
    @Operation(summary = "Receive Wompi webhook event", description = """
            Public endpoint used exclusively by Wompi to notify transaction status updates.
            Secured via SHA-256 signature validation instead of JWT.
            """)
    @ApiResponse(responseCode = "200", description = "Webhook event successfully received and processed")
    @ApiResponse(responseCode = "400", description = "Malformed webhook payload")
    @ApiResponse(responseCode = "401", description = "Invalid Wompi signature")
    @ApiResponse(responseCode = "500", description = "Unexpected server error")
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
    @Operation(summary = "Create pending payment for an order", description = """
            Creates a new pending payment associated with the specified order.
            
            This endpoint **must be called before** opening the Wompi Checkout Widget.
            The returned `reference` is required by Wompi to correlate the transaction.
            
            ## PSE — Redirect Required
            
            For PSE payments the response includes a `redirectUrl`.
            For `CARD` and `NEQUI`, `redirectUrl` is absent.
            """)
    @ApiResponse(
            responseCode = "201",
            description = "Pending payment created — ready to initiate Wompi Checkout",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.PaymentResponseBody.class),
                    examples = @ExampleObject(name = "card-or-nequi", value = """
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
                            """)
            )
    )
    @ApiResponse(responseCode = "400", description = "Invalid request payload",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class)))
    @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(value = """
                            { "success": false,\s
                            "message": "Order not found"\s
                            }
                           \s""")))
    @ApiResponse(responseCode = "500", description = "Unexpected server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class)))
    public ResponseEntity<AppResponse<PaymentResponse>> createPayment(
            @Parameter(name = "orderId", description = "Unique identifier of the order to be paid",
                    required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID orderId,
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        PaymentResponse payment = paymentService.createPendingPayment(orderId, request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AppResponse.success("Payment created successfully", payment));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get payment status by order", description = """
            Retrieves the current payment record associated with the specified order.
            
            ## Polling Strategy
            
            Poll every **3 seconds** with a max timeout of **5 minutes** until
            a final state is reached: `APPROVED`, `DECLINED`, `ERROR`, or `VOIDED`.
            """)
    @ApiResponse(
            responseCode = "200",
            description = "Payment record found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.PaymentResponseBody.class),
                    examples = @ExampleObject(name = "approved", value = """
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
                            """)
            )
    )
    @ApiResponse(responseCode = "404", description = "No payment found for the specified order",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(value = """
                            { "success": false,\s
                            "message": "Payment not found for order"\s
                            }
                           \s""")))
    public ResponseEntity<AppResponse<PaymentResponse>> getPaymentByOrder(
            @Parameter(name = "orderId", description = "Unique identifier of the order",
                    required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(AppResponse.success(
                "Payment retrieved successfully",
                paymentService.findByOrderId(orderId)
        ));
    }

    @GetMapping("/financial-institutions")
    @Operation(summary = "List available PSE financial institutions", description = """
            Retrieves the list of financial institutions currently supported by Wompi for PSE.
            Call this before rendering the PSE payment form to populate the bank selector.
            """)
    @ApiResponse(
            responseCode = "200",
            description = "Financial institutions retrieved successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SwaggerResponses.FinancialInstitutionsResponseBody.class),
                    examples = @ExampleObject(name = "institutions-list", value = """
                            {
                              "success": true,
                              "message": "Financial institutions retrieved successfully",
                              "data": [
                                { "financial_institution_code": "1007", "financial_institution_name": "Bancolombia" },
                                { "financial_institution_code": "1022", "financial_institution_name": "Banco de Bogotá" }
                              ]
                            }
                            """)
            )
    )
    @ApiResponse(responseCode = "500", description = "Unable to retrieve financial institutions — Wompi error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AppResponse.class)))
    public ResponseEntity<AppResponse<List<WompiFinancialInstitutionsResponse>>> getFinancialInstitutions() {
        return ResponseEntity.ok(AppResponse.success(
                "Financial institutions retrieved successfully",
                paymentService.getFinancialInstitutions()
        ));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}