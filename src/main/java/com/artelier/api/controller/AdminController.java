package com.artelier.api.controller;

import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.UserResponse;
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
import org.springframework.data.domain.Page;
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
        Administrative operations restricted to users with the ADMIN role.

        ## User Moderation

        Administrators can ban or unban platform accounts.

        ## User Listing

        Administrators can retrieve a paginated list of users.
        Each user is represented by UserResponse, which excludes
        internal fields such as password hashes and authentication tokens.

        ## Security

        All endpoints require a valid JWT and the ADMIN role.
        Requests without sufficient authorization may be rejected
        with HTTP 401 or HTTP 403, depending on the security configuration.
        """
)
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @Operation(
            summary = "Ban user",
            description = """
            Marks the specified user account as banned.

            ## Behavior

            Sets the user's banned flag to true.
            The operation does not delete the account or its associated data.

            Repeating the operation for an already banned user leaves
            the account in the banned state.

            ## Authorization

            Requires the ADMIN role.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "User successfully banned",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "user-banned",
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
            responseCode = "400",
            description = "Invalid user UUID",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                            {
                              "success": false,
                              "message": "Invalid value for parameter 'id'"
                            }
                            """
                    )
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
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
                    description = "UUID of the user to ban",
                    required = true,
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
            Removes the banned flag from the specified user account.

            ## Behavior

            Sets the user's banned flag to false.
            The operation does not delete the account or its associated data.

            Repeating the operation for an already unbanned user leaves
            the account in the unbanned state.

            ## Authorization

            Requires the ADMIN role.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "User successfully unbanned",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "user-unbanned",
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
            responseCode = "400",
            description = "Invalid user UUID",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                            {
                              "success": false,
                              "message": "Invalid value for parameter 'id'"
                            }
                            """
                    )
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
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
                    description = "UUID of the user to unban",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id
    ) {
        adminService.setBanned(id, false);
        return ResponseEntity.ok(AppResponse.success("User unbanned"));
    }

    @Operation(
            summary = "List users",
            description = """
            Retrieves a paginated list of registered users.

            ## Pagination

            Results are returned using Spring Data's Page structure.
            Page numbering starts at zero.

            - Default page: 0
            - Default page size: 20
            - Minimum page size: 1
            - Maximum page size: 100

            ## Returned Data

            Each item contains the user's UUID, email, full name, phone,
            banned status, and role.

            Password hashes, refresh tokens, and order entities are
            not included in UserResponse.

            ## Authorization

            Requires the ADMIN role.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            name = "users-page",
                            summary = "Paginated list of users",
                            value = """
                            {
                              "success": true,
                              "message": "Users retrieved successfully",
                              "data": {
                                "content": [
                                  {
                                    "id": "550e8400-e29b-41d4-a716-446655440000",
                                    "email": "user@example.com",
                                    "fullName": "Example User",
                                    "phone": "3001234567",
                                    "banned": false,
                                    "role": "USER"
                                  }
                                ],
                                "pageable": {
                                  "pageNumber": 0,
                                  "pageSize": 20,
                                  "sort": {
                                    "empty": true,
                                    "sorted": false,
                                    "unsorted": true
                                  },
                                  "offset": 0,
                                  "paged": true,
                                  "unpaged": false
                                },
                                "last": true,
                                "totalElements": 1,
                                "totalPages": 1,
                                "size": 20,
                                "number": 0,
                                "sort": {
                                  "empty": true,
                                  "sorted": false,
                                  "unsorted": true
                                },
                                "first": true,
                                "numberOfElements": 1,
                                "empty": false
                              }
                            }
                            """
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid pagination parameters",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "negative-page",
                                    summary = "Page index cannot be negative",
                                    value = """
                                    {
                                      "success": false,
                                      "message": "Page index cannot be negative"
                                    }
                                    """
                            ),
                            @ExampleObject(
                                    name = "invalid-size",
                                    summary = "Page size outside allowed range",
                                    value = """
                                    {
                                      "success": false,
                                      "message": "Page size must be between 1 and 100"
                                    }
                                    """
                            ),
                            @ExampleObject(
                                    name = "invalid-type",
                                    summary = "Non-numeric pagination parameter",
                                    value = """
                                    {
                                      "success": false,
                                      "message": "Invalid value for parameter 'page'"
                                    }
                                    """
                            )
                    }
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppResponse.class),
                    examples = @ExampleObject(
                            value = """
                            {
                              "success": false,
                              "message": "Access denied"
                            }
                            """
                    )
            )
    )
    @Parameter(
            name = "page",
            description = "Zero-based page index",
            example = "0",
            schema = @Schema(
                    type = "integer",
                    minimum = "0",
                    defaultValue = "0"
            )
    )
    @Parameter(
            name = "size",
            description = "Number of users per page. Allowed range: 1–100.",
            example = "20",
            schema = @Schema(
                    type = "integer",
                    minimum = "1",
                    maximum = "100",
                    defaultValue = "20"
            )
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<AppResponse<Page<UserResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<UserResponse> users = adminService.listUsers(page, size);

        return ResponseEntity.ok(
                AppResponse.success(
                        "Users retrieved successfully",
                        users
                )
        );
    }
}