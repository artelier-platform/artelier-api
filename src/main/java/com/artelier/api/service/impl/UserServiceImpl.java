package com.artelier.api.service.impl;

import com.artelier.api.dto.response.UserResponse;
import com.artelier.api.entity.User;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.mapper.UserMapper;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public void setBanned(UUID userId, boolean banned) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        ArtelierException.notFound("User not found"));

        user.setBanned(banned);
        userRepository.save(user);
    }

    @Override
    public Page<UserResponse> listUsers(int page, int size) {
        if (page < 0) {
            throw ArtelierException.badRequest(
                    "Page index cannot be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw ArtelierException.badRequest(
                    "Page size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(page, size);

        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }
}
