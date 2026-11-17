package com.artelier.api.user.controller;

import com.artelier.api.user.dto.UpdateUserProfileRequest;
import com.artelier.api.shared.dto.AppResponse;
import com.artelier.api.user.dto.UserProfileResponse;
import com.artelier.api.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.artelier.api.user.entity.User;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<AppResponse<UserProfileResponse>> getProfile(
            @PathVariable String userId
    ) {
        UserProfileResponse profile = userService.getProfile(java.util.UUID.fromString(userId));
        return ResponseEntity.ok(AppResponse.success("User profile retrieved successfully", profile));
    }

    @PutMapping("/{userId}/profile")
    public ResponseEntity<AppResponse<UserProfileResponse>> updateProfile(
            @PathVariable String userId,
            @RequestBody UpdateUserProfileRequest request
    ){
        UserProfileResponse updatedProfile = userService.updateProfile(java.util.UUID.fromString(userId), request);
        return ResponseEntity.ok(AppResponse.success("User profile updated successfully", updatedProfile));
    }
}
