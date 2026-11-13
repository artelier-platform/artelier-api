package com.artelier.api.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request used to confirm ownership of a phone number through SMS verification.
 *
 * <p>The verification code must match a valid, unexpired code
 * previously issued for the authenticated user's phone number.</p>
 *
 * <p>Upon successful verification, the server records the verification
 * timestamp in the user's phone verification information.</p>
 *
 * @param code numeric verification code received through SMS
 */
@Schema(description = "Request used to confirm phone number verification")
public record VerifyPhoneRequest(

        @Schema(
                description = "Numeric verification code received through SMS",
                example = "482951",
                maxLength = 10,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Pattern(regexp = "^[0-9]+$")
        @Size(max = 10)
        String code

) {
    @Override
    public String toString() {
        return "VerifyPhoneRequest[code=***]";
    }
}