package com.artelier.api.user.service.impl;

import com.artelier.api.user.dto.UpdateUserProfileRequest;
import com.artelier.api.user.dto.UserProfileResponse;
import com.artelier.api.user.mapper.UserMapper;
import com.artelier.api.user.repository.UserRepository;
import com.artelier.api.user.service.UserService;
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
