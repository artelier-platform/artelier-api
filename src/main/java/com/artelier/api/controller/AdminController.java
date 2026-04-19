package com.artelier.api.controller;

import com.artelier.api.dto.response.ApiResponse;
import com.artelier.api.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/admin")
@Tag(
        name = "Admin",
        description = "Admin-only operations for managing users"
)
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @Operation(
            summary = "Ban user",
            description = "Marks a user as banned. Only accessible to ADMIN users."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User banned successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Access denied"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}/ban")
    public ResponseEntity<ApiResponse<Void>> banUser(
            @Parameter(
                    description = "User UUID",
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id
    ) {
        adminService.setBanned(id, true);
        return ResponseEntity.ok(ApiResponse.success("User banned"));
    }

    @Operation(
            summary = "Unban user",
            description = "Removes banned status from a user"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User unbanned successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Access denied"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}/unban")
    public ResponseEntity<ApiResponse<Void>> unbanUser(
            @Parameter(
                    description = "User UUID",
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id
    ) {
        adminService.setBanned(id, false);
        return ResponseEntity.ok(ApiResponse.success("User unbanned"));
    }
}