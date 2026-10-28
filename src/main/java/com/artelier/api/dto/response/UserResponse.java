package com.artelier.api.dto.response;

import com.artelier.api.enums.Role;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        boolean banned,
        Role role
) {
}
