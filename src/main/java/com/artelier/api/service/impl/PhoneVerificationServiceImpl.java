package com.artelier.api.service.impl;

import com.artelier.api.dto.request.PhoneVerificationRequest;
import com.artelier.api.dto.request.VerifyPhoneRequest;
import com.artelier.api.repository.PhoneVerificationRepository;
import com.artelier.api.service.PhoneVerificationService;
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
