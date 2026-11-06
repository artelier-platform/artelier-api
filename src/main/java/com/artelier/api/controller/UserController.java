package com.artelier.api.controller;

import com.artelier.api.dto.request.UpdateUserProfileRequest;
import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.UserProfileResponse;
import com.artelier.api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
