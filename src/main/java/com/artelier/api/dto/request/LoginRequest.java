package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Login credentials")
public class LoginRequest {

    @Schema(
            description = "User email",
            example = "user@example.com"
    )
    @Email
    @NotBlank
    private String email;

    @Schema(
            description = "User password",
            example = "StrongPass123"
    )
    @NotBlank
    @Size(min = 8)
    private String password;
}