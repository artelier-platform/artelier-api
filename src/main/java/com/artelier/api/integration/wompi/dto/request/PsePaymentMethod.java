package com.artelier.api.integration.wompi.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PsePaymentMethod", description = "PSE bank transfer payment.")
public class PsePaymentMethod implements PaymentMethodBody {

    @Schema(
            description = "Payment method type.",
            example = "PSE",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private final String type = "PSE";

    @NotNull
    @JsonProperty("user_type")
    @Schema(
            description = "Person type. Natural person (0) or legal entity (1).",
            example = "0"
    )
    private Integer userType;

    @NotBlank
    @JsonProperty("user_legal_id_type")
    @Schema(
            description = "Document type (CC, CE, NIT, etc.).",
            example = "CC"
    )
    private String userLegalIdType;

    @NotBlank
    @JsonProperty("user_legal_id")
    @Schema(
            description = "Customer document number.",
            example = "1099888777"
    )
    private String userLegalId;

    @NotBlank
    @JsonProperty("financial_institution_code")
    @Schema(
            description = "Financial institution code returned by Wompi.",
            example = "1"
    )
    private String financialInstitutionCode;

    @NotBlank
    @Size(max = 64)
    @JsonProperty("payment_description")
    @Schema(
            description = "Payment description. Maximum 64 characters.",
            example = "Payment for Artelier order #123"
    )
    private String paymentDescription;

    @JsonProperty("reference_one")
    @Schema(
            description = "Customer IP address (financial services anti-fraud field).",
            example = "192.168.0.1"
    )
    private String referenceOne;

    @JsonProperty("reference_two")
    @Schema(
            description = "Financial product opening date in yyyyMMdd format.",
            example = "20240101"
    )
    private String referenceTwo;

    @JsonProperty("reference_three")
    @Schema(
            description = "Financial product beneficiary document number.",
            example = "12345678"
    )
    private String referenceThree;
}
