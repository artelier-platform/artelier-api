package com.artelier.api.service;

import com.artelier.api.dto.response.IssuedSession;
import com.artelier.api.entity.User;
import com.artelier.api.enums.SessionRevocationReason;

import java.util.UUID;

public interface UserSessionService {
    IssuedSession create(User user);
    IssuedSession rotate(String refreshToken);
    void revoke(String refreshToken, SessionRevocationReason reason);
    boolean isActive(UUID sessionId, UUID userId);
}
