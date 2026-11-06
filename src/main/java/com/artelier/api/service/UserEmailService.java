package com.artelier.api.service;

import com.artelier.api.dto.response.UserEmailResponse;
import com.artelier.api.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserEmailService {
    UserEmailResponse addEmail(UUID userId, String email);
    Optional<User> findVerifiedOwner(String email);
    UserEmailResponse verifyEmail(String token);
    UserEmailResponse setPrimary(UUID userId, UUID emailId);
    void removeEmail(UUID userId, UUID emailId);
}