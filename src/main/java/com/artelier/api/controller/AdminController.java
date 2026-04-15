package com.artelier.api.controller;

import com.artelier.api.dto.response.ApiResponse;
import com.artelier.api.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Admin-only operations")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    @Autowired
    private AdminService adminService;


    @Operation(summary = "Ban user")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}/ban")
    public ResponseEntity<ApiResponse<Void>> banUser(@PathVariable UUID id) {
        adminService.setBanned(id, true);
        return ResponseEntity.ok(ApiResponse.success("User banned"));
    }

    @Operation(summary = "Unban user")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}/unban")
    public ResponseEntity<ApiResponse<Void>> unbanUser(@PathVariable UUID id) {
        adminService.setBanned(id, false);
        return ResponseEntity.ok(ApiResponse.success("User unbanned"));
    }
}