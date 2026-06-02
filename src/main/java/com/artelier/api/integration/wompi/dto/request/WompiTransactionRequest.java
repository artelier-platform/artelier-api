package com.artelier.api.integration.wompi.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request used to create a Wompi transaction.")
public class WompiTransactionRequest  {

    @NotBlank
    @JsonProperty("acceptance_token")
    @Schema(
            description = "Acceptance token obtained from Wompi terms and conditions.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "acceptance_token_example"
    )
    private String acceptanceToken;

    @JsonProperty("accept_personal_auth")
    @Schema(
            description = "Personal data authorization acceptance token when required by Wompi.",
            example = "personal_auth_token"
    )
    private String acceptPersonalAuth;

    @NotNull
    @JsonProperty("amount_in_cents")
    @Schema(
            description = "Transaction amount in cents.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "100000"
    )
    private Long amountInCents;

    @NotBlank
    @Schema(
            description = "Transaction currency. Currently only COP is supported.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "COP"
    )
    private String currency;

    @NotBlank
    @Email
    @JsonProperty("customer_email")
    @Schema(
            description = "Customer email address used to send payment receipts.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "customer@email.com"
    )
    private String customerEmail;

    @NotBlank
    @Size(max = 255)
    @Schema(
            description = "Unique transaction reference in the merchant system. Must be unique per transaction.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "ORDER-550e8400-e29b-41d4-a716-446655440000"
    )
    private String reference;

    @NotBlank
    @Schema(
            description = "Integrity signature generated using the merchant integrity secret.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "d2f93e7cf8f8f0d1d9f0..."
    )
    private String signature;

    @Valid
    @NotNull
    @JsonProperty("payment_method")
    @Schema(
            description = "Payment method information.",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private PaymentMethodBody paymentMethod;

    @JsonProperty("customer_data")
    @Schema(
            description = "Optional customer information such as name, phone number and address."
    )
    private CustomerData customerData;

    @JsonProperty("redirect_url")
    @Schema(
            description = "URL where the customer will be redirected after payment.",
            example = "https://example.com/payment/result"
    )
    private String redirectUrl;

    @Schema(
            description = "IP address of the device used to create the transaction.",
            example = "190.25.10.15"
    )
    private String ip;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Customer additional information.")
    public static class CustomerData {

        @JsonProperty("full_name")
        @Schema(example = "María García López")
        private String fullName;

        @Schema(example = "María")
        private String firstName;

        @Schema(example = "García")
        private String lastName;

        @JsonProperty("phone_number")
        @Schema(example = "+573001112233")
        private String phoneNumber;

        @Schema(example = "Bogotá")
        private String city;

        @Schema(example = "Colombia")
        private String country;

        @Schema(example = "Calle 123 #45-67")
        private String address;
    }
}