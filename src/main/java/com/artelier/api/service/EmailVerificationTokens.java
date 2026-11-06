package com.artelier.api.service;

import java.util.UUID;

public interface EmailVerificationTokens {
    String issue(UUID emailId);
    UUID parse(String token);              // 400 si es inválido o expiró
}
