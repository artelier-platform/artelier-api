
package com.artelier.api.dto.response;

import com.artelier.api.integration.wompi.enums.PaymentMethod;
import com.artelier.api.integration.wompi.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(
        name = "PaymentResponse",
        description = "Payment response payload"
)
public record PaymentResponse(

        @Schema(
                description = "Unique identifier of the payment",
                example = "550e8400-e29b-41d4-a716-446655440000",
                format = "uuid",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Identifier of the order being paid",
                example = "660e8400-e29b-41d4-a716-446655440001",
                format = "uuid",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID orderId,

        @Schema(
                description = "Transaction identifier assigned by Wompi",
                example = "1292-1602113476-10985",
                nullable = true
        )
        String wompiTransactionId,

        @Schema(
                description = "Merchant reference used to correlate the Wompi transaction",
                example = "ORDER-550e8400-1715000000000",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String reference,

        @Schema(
                description = "Current payment status",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        PaymentStatus status,

        @Schema(
                description = "Payment amount",
                example = "120000.00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        BigDecimal amount,

        @Schema(
                description = "Payment method used by the customer",
                example = "NEQUI",
                nullable = true
        )
        PaymentMethod paymentMethod,

        @Schema(
                description = "Redirect URL for PSE or Bancolombia",
                nullable = true
        )
        String redirectUrl,

        @Schema(
                description = "Timestamp when the payment was confirmed",
                example = "2026-05-07T00:00:00Z",
                format = "date-time",
                nullable = true
        )
        Instant paidAt

) {}
