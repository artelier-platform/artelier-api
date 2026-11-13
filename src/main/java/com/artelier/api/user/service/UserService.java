
package com.artelier.api.user.service;

import com.artelier.api.user.dto.UpdateUserProfileRequest;
import com.artelier.api.user.dto.UserProfileResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface UserService {
    void suspend(UUID userId);
    void activate(UUID userId);
    Page<UserProfileResponse> listUsers(int page, int size);
    UserProfileResponse getProfile(UUID userId);
    UserProfileResponse updateProfile(UUID userId, UpdateUserProfileRequest request);
}
