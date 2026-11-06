package com.artelier.api.dto.response;

import com.artelier.api.entity.UserSession;

public record IssuedSession(UserSession session, String refreshToken) {}