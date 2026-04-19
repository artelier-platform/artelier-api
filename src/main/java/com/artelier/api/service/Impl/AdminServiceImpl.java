package com.artelier.api.service.Impl;

import com.artelier.api.entity.User;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.service.AdminService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@AllArgsConstructor
@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;


    public void setBanned(UUID userId, boolean banned) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ArtelierException.notFound("User not found"));
        user.setBanned(banned);
        userRepository.save(user);
    }
}
