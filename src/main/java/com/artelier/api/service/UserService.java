
package com.artelier.api.service;

import com.artelier.api.dto.request.UpdateUserProfileRequest;
import com.artelier.api.dto.response.UserProfileResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface UserService {
    void suspend(UUID userId);
    void activate(UUID userId);
    Page<UserProfileResponse> listUsers(int page, int size);
    UserProfileResponse getProfile(UUID userId);
    UserProfileResponse updateProfile(UUID userId, UpdateUserProfileRequest request);
}
