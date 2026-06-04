package com.artelier.api.controller;

import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/admin")
@Tag(
        name = "Admin",
        description = """
        Administrative operations restricted to users with the `ADMIN` role.
        
        ## User Moderation
        
        Administrators can temporarily restrict or restore access to platform accounts.
        
        ## Ban Behavior
        
        When a user is banned:
        
        - The account is marked as restricted.
        - The user may be prevented from accessing protected features.
        - Existing account data remains unchanged.
        - The operation is reversible through the unban endpoint.
        
        ## Security
        
        All endpoints in this section require:
        
        - Valid JWT authentication
        - `ADMIN` role
        
        Requests made by non-admin users are rejected with `HTTP 403 Forbidden`.
        """
)
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @Operation(
            summary = "Ban user",
            description = """
        Restricts a user account by marking it as banned.
        
        ## Purpose
        
        This operation is intended for moderation and administrative actions.
        
        Once banned, the user account is flagged internally and may lose access
        to authenticated functionality depending on platform authorization rules.
        
        ## Idempotency
        
        Calling this endpoint multiple times for an already banned user
        produces the same final state.
        
        ## Authorization
        
        Only users with the `ADMIN` role can perform this action.
        
        ## Audit Impact
        
        User data is preserved. Only the moderation status is updated.
        """
    )
    @ApiResponse(
            responseCode = "200",
            description = "User successfully marked as banned",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "user-banned",
                            summary = "User moderation status updated successfully",
                            value = """
                                {
                                  "success": true,
                                  "message": "User banned"
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Access denied — ADMIN role required",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "forbidden",
                            value = """
                                {
                                  "success": false,
                                  "message": "Access denied"
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "user-not-found",
                            value = """
                                {
                                  "success": false,
                                  "message": "User not found"
                                }
                                """
                    )
            )
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}/ban")
    public ResponseEntity<AppResponse<Void>> banUser(
            @Parameter(
                    description = "User UUID",
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id
    ) {
        adminService.setBanned(id, true);
        return ResponseEntity.ok(AppResponse.success("User banned"));
    }

    @Operation(
            summary = "Unban user",
            description = """
        Restores access to a previously banned user account.
        
        ## Purpose
        
        Removes the banned flag from the specified user and restores the account
        to its normal operational state.
        
        ## Idempotency
        
        Calling this endpoint multiple times for a user that is already active
        produces the same final state.
        
        ## Authorization
        
        Only users with the `ADMIN` role can perform this action.
        
        ## Audit Impact
        
        User data is preserved. Only the moderation status is updated.
        """
    )
    @ApiResponse(
            responseCode = "200",
            description = "User successfully restored",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "user-unbanned",
                            summary = "User moderation restriction removed",
                            value = """
                                {
                                  "success": true,
                                  "message": "User unbanned"
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Access denied — ADMIN role required",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "forbidden",
                            value = """
                                {
                                  "success": false,
                                  "message": "Access denied"
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "user-not-found",
                            value = """
                                {
                                  "success": false,
                                  "message": "User not found"
                                }
                                """
                    )
            )
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}/unban")
    public ResponseEntity<AppResponse<Void>> unbanUser(
            @Parameter(
                    description = "User UUID",
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id
    ) {
        adminService.setBanned(id, false);
        return ResponseEntity.ok(AppResponse.success("User unbanned"));
    }
}