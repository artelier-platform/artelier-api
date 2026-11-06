package com.artelier.api.service.impl;

import com.artelier.api.enums.UserStatus;
import com.artelier.api.service.AccountVerificationService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AccountVerificationServiceImpl implements AccountVerificationService {
    @Override
    public UserStatus activateIfEligible(UUID userId) {
        throw new UnsupportedOperationException("TODO");
    }
}
