package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "User registration request")
public class RegisterRequest {

    @Schema(
            example = "user@example.com"
    )
    @Email
    @NotBlank
    private String email;

    @Schema(
            example = "StrongPass123"
    )
    @NotBlank
    @Size(min = 8)
    private String password;

    @Schema(
            example = "John Doe"
    )
    @NotBlank
    private String fullName;

    @Schema(
            example = "+573001234567"
    )
    private String phone;
}