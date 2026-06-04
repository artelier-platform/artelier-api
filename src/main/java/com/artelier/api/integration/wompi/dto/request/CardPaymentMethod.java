package com.artelier.api.integration.wompi.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CardPaymentMethod", description = "Credit or debit card payment.")
public class CardPaymentMethod implements PaymentMethodBody {

    @Schema(description = "Payment method type.", example = "CARD",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private final String type = "CARD";

    @NotBlank
    @Schema(description = "Card token generated previously by Wompi.",
            example = "tok_prod_123456")
    private String token;

    @Min(1)
    @Schema(description = "Number of installments selected by the customer.",
            example = "1")
    private Integer installments;
}