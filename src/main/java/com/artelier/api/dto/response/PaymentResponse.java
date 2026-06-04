package com.artelier.api.dto.response;

import com.artelier.api.integration.wompi.enums.PaymentMethod;
import com.artelier.api.integration.wompi.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Schema(description = "Payment response payload.")
public class PaymentResponse {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID orderId;

    @Schema(example = "1292-1602113476-10985")
    private String wompiTransactionId;

    @Schema(example = "ORDER-550e8400-1715000000000")
    private String reference;

    @Schema(description = "Current payment status.")
    private PaymentStatus status;

    @Schema(example = "120000.00")
    private BigDecimal amount;

    @Schema(example = "NEQUI")
    private PaymentMethod paymentMethod;

    @Schema(description = "Redirect URL for PSE or Bancolombia. Frontend must redirect user here.")
    private String redirectUrl;

    private Instant paidAt;
}