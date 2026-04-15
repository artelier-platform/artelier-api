package com.artelier.api.service;

import java.util.UUID;

public interface AdminService {
    void setBanned(UUID userId, boolean banned);
}
