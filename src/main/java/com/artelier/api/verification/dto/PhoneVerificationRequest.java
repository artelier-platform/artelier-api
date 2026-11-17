package com.artelier.api.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


/**
 * Request used to initiate phone number verification through SMS.
 *
 * <p>The request contains the international calling code and
 * the local phone number.</p>
 *
 * <p>The server sends a verification code to the provided phone number.
 * The phone number must be associated with the authenticated user
 * before it can be considered verified.</p>
 *
 * <p>Requesting a new verification may invalidate previously issued codes,
 * depending on the verification service implementation.</p>
 *
 * @param phoneCountryCode international dialing code, including the plus sign
 * @param phoneNumber      local phone number containing digits only
 */
@Schema(description = "Request used to initiate phone number verification")
public record PhoneVerificationRequest(

        @Schema(
                description = "International phone country calling code",
                example = "+57",
                maxLength = 4,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Pattern(regexp = "^\\+[1-9][0-9]{0,2}$")
        @Size(max = 4)
        String phoneCountryCode,

        @Schema(
                description = "Phone number without country calling code. Digits only.",
                example = "3001234567",
                maxLength = 15,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Pattern(regexp = "^[0-9]{1,15}$")
        String phoneNumber

) { }