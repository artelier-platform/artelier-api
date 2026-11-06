package com.artelier.api.service.impl;

import com.artelier.api.dto.request.UpdateUserProfileRequest;
import com.artelier.api.dto.response.UserProfileResponse;
import com.artelier.api.mapper.UserMapper;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.UUID;

@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;


    @Override
    public void suspend(UUID userId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void activate(UUID userId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Page<UserProfileResponse> listUsers(int page, int size) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public UserProfileResponse getProfile(UUID userId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public UserProfileResponse updateProfile(UUID userId, UpdateUserProfileRequest request) {
        throw new UnsupportedOperationException("TODO");
    }
}
