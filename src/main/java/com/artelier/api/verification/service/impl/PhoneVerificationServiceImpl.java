package com.artelier.api.verification.service.impl;

import com.artelier.api.verification.dto.PhoneVerificationRequest;
import com.artelier.api.verification.dto.VerifyPhoneRequest;
import com.artelier.api.verification.repository.PhoneVerificationRepository;
import com.artelier.api.verification.service.PhoneVerificationService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PhoneVerificationServiceImpl implements PhoneVerificationService {


    @Override
    public void requestVerification(UUID userId, PhoneVerificationRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void verify(UUID userId, VerifyPhoneRequest request) {
        throw new UnsupportedOperationException("TODO");
    }
}
