package com.artelier.api.user.controller;

import com.artelier.api.auth.security.UserPrincipal;
import com.artelier.api.shared.dto.AppResponse;
import com.artelier.api.user.dto.AddUserEmailRequest;
import com.artelier.api.user.dto.SetPrimaryEmailRequest;
import com.artelier.api.user.dto.UserEmailResponse;
import com.artelier.api.user.service.UserEmailService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(
        name = "User Emails",
        description = """
        Endpoints for managing the email addresses associated with the
        authenticated user's account.

        ## Access

        Email management endpoints require a valid Bearer JWT unless
        explicitly documented as public.

        ## Email rules

        - A user can have a maximum of **3 email addresses**.
        - Every newly added email starts as **unverified**.
        - An email must be verified before it can become the primary email.
        - The primary email cannot be removed.
        """
)
@SecurityRequirement(name = "bearerAuth")
public class UserEmailController {

    private static final String DESC_UNAUTHORIZED =
            "Authentication required: the Bearer token is missing, expired or invalid";

    private static final String DESC_BAD_REQUEST =
            "The request contains invalid data or the email cannot be used for this operation";

    private static final String DESC_CONFLICT =
            "The email is already registered or the user has reached the maximum number of emails";

    private static final String DESC_NOT_FOUND =
            "The requested email address does not exist";

    private static final String DESC_FORBIDDEN =
            "The authenticated user is not allowed to perform this operation";

    private static final String DESC_SERVER_ERROR =
            "Unexpected server error";

    private final UserEmailService userEmailService;

    // =========================================================
    // ADD EMAIL
    // =========================================================

    @Operation(
            summary = "Add an email address",
            description = """
                    Adds a new email address to the authenticated user's account.

                    The email is created as unverified and a verification message
                    is sent to the provided address.

                    A user can have a maximum of three email addresses.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
            responseCode = "200",
            description = "Email address added successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": true,
                                      "message": "Email added successfully",
                                      "data": {
                                        "id": "550e8400-e29b-41d4-a716-446655440000",
                                        "email": "secondary@example.com",
                                        "isPrimary": false,
                                        "isVerified": false,
                                        "verifiedAt": null
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = DESC_BAD_REQUEST
    )
    @ApiResponse(
            responseCode = "401",
            description = DESC_UNAUTHORIZED
    )
    @ApiResponse(
            responseCode = "403",
            description = DESC_FORBIDDEN
    )
    @ApiResponse(
            responseCode = "409",
            description = DESC_CONFLICT
    )
    @ApiResponse(
            responseCode = "500",
            description = DESC_SERVER_ERROR
    )
    @PostMapping(
            value = "/emails",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AppResponse<UserEmailResponse>> addEmail(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddUserEmailRequest request
    ) {

        UUID userId = principal.getUser().getId();

        UserEmailResponse response =
                userEmailService.addEmail(userId, request.email());

        return ResponseEntity.ok(
                AppResponse.success(
                        "Email added successfully",
                        response
                )
        );
    }

    // =========================================================
    // VERIFY EMAIL
    // =========================================================

    @Operation(
            summary = "Verify an email address",
            description = """
                    Verifies ownership of an email address using the token
                    received by email.

                    This endpoint is public because the user may not yet have
                    an authenticated session when following the verification link.
                    """,
            security = {}
    )
    @ApiResponse(
            responseCode = "200",
            description = "Email address verified successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "The verification token is invalid or expired"
    )
    @ApiResponse(
            responseCode = "404",
            description = DESC_NOT_FOUND
    )
    @ApiResponse(
            responseCode = "500",
            description = DESC_SERVER_ERROR
    )
    @SecurityRequirements
    @GetMapping(
            value = "/emails/verify",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AppResponse<UserEmailResponse>> verifyEmail(
            @RequestParam String token
    ) {

        UserEmailResponse response =
                userEmailService.verifyEmail(token);

        return ResponseEntity.ok(
                AppResponse.success(
                        "Email verified successfully",
                        response
                )
        );
    }

    // =========================================================
    // SET PRIMARY EMAIL
    // =========================================================

    @Operation(
            summary = "Set primary email",
            description = """
                    Changes the primary email address of the authenticated user.

                    The selected email must:
                    - belong to the authenticated user;
                    - already be verified.

                    The previously primary email is automatically demoted.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
            responseCode = "200",
            description = "Primary email updated successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = DESC_BAD_REQUEST
    )
    @ApiResponse(
            responseCode = "401",
            description = DESC_UNAUTHORIZED
    )
    @ApiResponse(
            responseCode = "404",
            description = DESC_NOT_FOUND
    )
    @ApiResponse(
            responseCode = "500",
            description = DESC_SERVER_ERROR
    )
    @PutMapping(
            value = "/emails/{emailId}/primary",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AppResponse<UserEmailResponse>> setPrimary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID emailId
    ) {

        UUID userId = principal.getUser().getId();

        UserEmailResponse response =
                userEmailService.setPrimary(userId, emailId);

        return ResponseEntity.ok(
                AppResponse.success(
                        "Primary email updated successfully",
                        response
                )
        );
    }

    // =========================================================
    // REMOVE EMAIL
    // =========================================================

    @Operation(
            summary = "Remove an email address",
            description = """
                    Removes a secondary email address from the authenticated
                    user's account.

                    The primary email cannot be removed.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
            responseCode = "204",
            description = "Email address removed successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = DESC_BAD_REQUEST
    )
    @ApiResponse(
            responseCode = "401",
            description = DESC_UNAUTHORIZED
    )
    @ApiResponse(
            responseCode = "404",
            description = DESC_NOT_FOUND
    )
    @ApiResponse(
            responseCode = "500",
            description = DESC_SERVER_ERROR
    )
    @DeleteMapping("/emails/{emailId}")
    public ResponseEntity<Void> removeEmail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID emailId
    ) {

        UUID userId = principal.getUser().getId();

        userEmailService.removeEmail(userId, emailId);

        return ResponseEntity.noContent().build();
    }
}