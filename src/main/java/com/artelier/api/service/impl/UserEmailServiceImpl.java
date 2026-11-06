package com.artelier.api.service.impl;

import com.artelier.api.dto.response.UserEmailResponse;
import com.artelier.api.entity.User;
import com.artelier.api.service.UserEmailService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserEmailServiceImpl implements UserEmailService {
    @Override
    public UserEmailResponse addEmail(UUID userId, String email) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Optional<User> findVerifiedOwner(String email) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public UserEmailResponse verifyEmail(String token) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public UserEmailResponse setPrimary(UUID userId, UUID emailId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void removeEmail(UUID userId, UUID emailId) {
        throw new UnsupportedOperationException("TODO");
    }
}
