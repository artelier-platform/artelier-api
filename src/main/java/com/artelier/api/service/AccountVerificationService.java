package com.artelier.api.service;

import com.artelier.api.enums.UserStatus;

import java.util.UUID;

public interface AccountVerificationService {
    UserStatus activateIfEligible(UUID userId);
}
