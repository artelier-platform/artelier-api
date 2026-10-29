
package com.artelier.api.service;

import com.artelier.api.dto.response.UserResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface UserService {

    void setBanned(UUID userId, boolean banned);

    Page<UserResponse> listUsers(int page, int size);
}
