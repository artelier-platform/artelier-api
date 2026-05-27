package com.artelier.api.dto.response;

import com.artelier.api.entity.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Schema(description = "Payment response payload")
public class PaymentResponse {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID orderId;

    @Schema(example = "wompi_tx_12345")
    private String wompiTransactionId;

    @Schema(description = "Current payment status", allowableValues = {"PENDING", "APPROVED", "DECLINED", "ERROR"})
    private PaymentStatus status;

    @Schema(example = "120000.00")
    private BigDecimal amount;

    @Schema(example = "NEQUI")
    private String paymentMethod;

    private Instant paidAt;
}