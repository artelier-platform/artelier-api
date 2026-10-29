package com.artelier.api.dto.request;

import com.artelier.api.integration.wompi.dto.request.PaymentMethodBody;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request to initiate a payment for an order.
 *
 * <p>JSON property names follow Wompi's snake_case convention
 * ({@code payment_method}, {@code customer_data}).</p>
 *
 * @param paymentMethod payment method chosen by the customer (required)
 * @param customerData  optional customer data; required for PSE payments
 */
@Schema(description = "Request to initiate a payment for an order")
public record PaymentRequest(

        @Schema(
                description = "Payment method chosen by the customer",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Valid
        @NotNull
        @JsonProperty("payment_method")
        PaymentMethodBody paymentMethod,

        @Schema(description = "Optional customer info (name, phone). Required for PSE.")
        @JsonProperty("customer_data")
        CustomerData customerData

) {

    /**
     * Customer personal data.
     *
     * @param fullName    full name of the customer
     * @param phoneNumber phone number of the customer, including country code
     */
    @Schema(description = "Customer personal data")
    public record CustomerData(

            @Schema(description = "Full name of the customer", example = "María García")
            @JsonProperty("full_name")
            String fullName,

            @Schema(
                    description = "Phone number of the customer, including country code",
                    example = "+573001112233"
            )
            @JsonProperty("phone_number")
            String phoneNumber

    ) {}
}
