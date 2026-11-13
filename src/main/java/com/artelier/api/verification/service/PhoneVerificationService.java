package com.artelier.api.verification.service;

import com.artelier.api.verification.dto.PhoneVerificationRequest;
import com.artelier.api.verification.dto.VerifyPhoneRequest;

import java.util.UUID;

public interface PhoneVerificationService {
    void requestVerification(UUID userId, PhoneVerificationRequest request);
    void verify(UUID userId, VerifyPhoneRequest request);
}