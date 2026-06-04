package com.artelier.api.dto.request;

import com.artelier.api.integration.wompi.dto.request.PaymentMethodBody;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to initiate a payment for an order.")
public class PaymentRequest {

    @Valid
    @NotNull
    @JsonProperty("payment_method")
    @Schema(
            description = "Payment method chosen by the customer.",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private PaymentMethodBody paymentMethod;

    @JsonProperty("customer_data")
    @Schema(description = "Optional customer info (name, phone). Required for PSE.")
    private CustomerData customerData;

    @Data
    @Schema(description = "Customer personal data.")
    public static class CustomerData {

        @JsonProperty("full_name")
        @Schema(example = "María García")
        private String fullName;

        @JsonProperty("phone_number")
        @Schema(example = "+573001112233")
        private String phoneNumber;
    }
}