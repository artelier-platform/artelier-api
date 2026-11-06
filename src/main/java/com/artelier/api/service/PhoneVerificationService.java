package com.artelier.api.service;

import com.artelier.api.dto.request.PhoneVerificationRequest;
import com.artelier.api.dto.request.VerifyPhoneRequest;

import java.util.UUID;

public interface PhoneVerificationService {
    void requestVerification(UUID userId, PhoneVerificationRequest request);
    void verify(UUID userId, VerifyPhoneRequest request);
}