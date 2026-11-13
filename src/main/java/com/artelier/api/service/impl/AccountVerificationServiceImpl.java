package com.artelier.api.service.impl;

import com.artelier.api.entity.User;
import com.artelier.api.entity.UserEmail;
import com.artelier.api.enums.UserStatus;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.repository.UserEmailRepository;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.service.AccountVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountVerificationServiceImpl
        implements AccountVerificationService {

    private final UserRepository userRepository;
    private final UserEmailRepository userEmailRepository;

    @Override
    public UserStatus activateIfEligible(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        ArtelierException.notFound(
                                "User with ID %s not found",
                                userId
                        )
                );

        boolean hasVerifiedEmail =
                userEmailRepository.findByUserId(userId)
                        .stream()
                        .anyMatch(UserEmail::isVerified);

        if (hasVerifiedEmail &&
                user.getStatus() == UserStatus.PENDING_VERIFICATION) {

            user.setStatus(UserStatus.ACTIVE);

            userRepository.save(user);
        }

        return user.getStatus();
    }
}
