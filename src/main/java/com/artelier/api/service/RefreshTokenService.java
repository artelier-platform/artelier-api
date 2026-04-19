package com.artelier.api.service;

import com.artelier.api.entity.RefreshToken;
import com.artelier.api.entity.User;

public interface RefreshTokenService {
    RefreshToken create(User user);
    RefreshToken validate(String token);
    void revokeByUser(User user);
}
