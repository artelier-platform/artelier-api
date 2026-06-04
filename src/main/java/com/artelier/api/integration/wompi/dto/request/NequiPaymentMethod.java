package com.artelier.api.integration.wompi.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "NequiPaymentMethod", description = "Nequi payment.")
public class NequiPaymentMethod implements PaymentMethodBody {

    @Schema(description = "Payment method type.", example = "NEQUI",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private final String type = "NEQUI";

    @NotBlank
    @JsonProperty("phone_number")
    @Schema(description = "Nequi account phone number.",
            example = "3107654321",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNumber;
}