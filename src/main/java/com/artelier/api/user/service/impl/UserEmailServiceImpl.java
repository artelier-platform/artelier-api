package com.artelier.api.user.service.impl;

import com.artelier.api.user.dto.UserEmailResponse;
import com.artelier.api.user.entity.User;
import com.artelier.api.user.entity.UserEmail;
import com.artelier.api.user.enums.UserStatus;
import com.artelier.api.shared.exception.ArtelierException;
import com.artelier.api.integration.mail.Service.VerificationMailSender;
import com.artelier.api.user.mapper.UserEmailMapper;
import com.artelier.api.user.repository.UserEmailRepository;
import com.artelier.api.user.repository.UserRepository;
import com.artelier.api.verification.service.AccountVerificationService;
import com.artelier.api.verification.service.EmailVerificationTokens;
import com.artelier.api.user.service.UserEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserEmailServiceImpl implements UserEmailService {

    private final UserRepository userRepository;
    private final UserEmailRepository userEmailRepository;
    private final UserEmailMapper userEmailMapper;
    private final EmailVerificationTokens tokens;
    private final VerificationMailSender mailSender;
    private final AccountVerificationService accountVerificationService;

    @Override
    public UserEmailResponse addEmail(UUID userId, String email) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ArtelierException.notFound(
                        "User with ID %s not found", userId
                ));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw ArtelierException.forbidden(
                    "User with ID %s is suspended", userId
            );
        }

        String normalizedEmail = email.trim().toLowerCase();

        if (userEmailRepository.existsByEmail(normalizedEmail)) {
            throw ArtelierException.conflict(
                    "Email %s is already registered",
                    normalizedEmail
            );
        }

        if (userEmailRepository.countByUserId(userId) >= 3) {
            throw ArtelierException.conflict(
                    "User with ID %s has reached the maximum number of emails",
                    userId
            );
        }

        UserEmail userEmail = UserEmail.builder()
                .email(normalizedEmail)
                .user(user)
                .isPrimary(false)
                .isVerified(false)
                .createdAt(Instant.now())
                .build();

        try {

            userEmail = userEmailRepository.saveAndFlush(userEmail);

        } catch (DataIntegrityViolationException ex) {

            throw ArtelierException.conflict(
                    "Email %s is already registered",
                    normalizedEmail
            );
        }

        String verificationToken =
                tokens.issue(userEmail.getId());

        mailSender.sendEmailVerification(
                userEmail.getEmail(),
                verificationToken
        );

        return userEmailMapper.toResponse(userEmail);
    }

    @Override
    public Optional<User> findVerifiedOwner(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        return userEmailRepository.findByEmail(normalizedEmail)
                .filter(UserEmail::isVerified)
                .map(UserEmail::getUser);
    }

    @Override
    public UserEmailResponse verifyEmail(String token) {

        UUID emailId = tokens.parse(token);

        UserEmail userEmail = userEmailRepository.findById(emailId)
                .orElseThrow(() -> ArtelierException.notFound(
                        "Email with ID %s not found",
                        emailId
                ));

        if (userEmail.isVerified()) {
            return userEmailMapper.toResponse(userEmail);
        }

        userEmail.setVerified(true);
        userEmail.setVerifiedAt(Instant.now());

        List<UserEmail> userEmails =
                userEmailRepository.findByUserId(
                        userEmail.getUser().getId()
                );

        boolean hasPrimary = userEmails.stream()
                .anyMatch(UserEmail::isPrimary);

        if (!hasPrimary) {
            userEmail.setPrimary(true);
        }

        userEmailRepository.saveAndFlush(userEmail);

        accountVerificationService.activateIfEligible(
                userEmail.getUser().getId()
        );

        return userEmailMapper.toResponse(userEmail);
    }

    @Override
    public UserEmailResponse setPrimary(UUID userId, UUID emailId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ArtelierException.notFound(
                        "User with ID %s not found",
                        userId
                ));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw ArtelierException.forbidden(
                    "User with ID %s is suspended",
                    userId
            );
        }

        UserEmail userEmail = userEmailRepository
                .findByIdAndUserId(emailId, userId)
                .orElseThrow(() -> ArtelierException.notFound(
                        "Email with ID %s not found for user with ID %s",
                        emailId,
                        userId
                ));

        if (!userEmail.isVerified()) {
            throw ArtelierException.conflict(
                    "Email with ID %s is not verified",
                    emailId
            );
        }

        if (userEmail.isPrimary()) {
            return userEmailMapper.toResponse(userEmail);
        }

        userEmailRepository.findByUserId(userId).stream()
                .filter(UserEmail::isPrimary)
                .forEach(email -> {
                    email.setPrimary(false);
                    userEmailRepository.saveAndFlush(email);
                });

        userEmail.setPrimary(true);
        userEmailRepository.saveAndFlush(userEmail);

        return userEmailMapper.toResponse(userEmail);
    }

    @Override
    public void removeEmail(UUID userId, UUID emailId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ArtelierException.notFound(
                        "User with ID %s not found",
                        userId
                ));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw ArtelierException.forbidden(
                    "User with ID %s is suspended",
                    userId
            );
        }

        UserEmail userEmail = userEmailRepository
                .findByIdAndUserId(emailId, userId)
                .orElseThrow(() -> ArtelierException.notFound(
                        "Email with ID %s not found for user with ID %s",
                        emailId,
                        userId
                ));

        if (userEmail.isPrimary()) {
            throw ArtelierException.conflict(
                    "Cannot remove primary email with ID %s",
                    emailId
            );
        }

        if (userEmailRepository.countByUserId(userId) <= 1) {
            throw ArtelierException.conflict(
                    "User with ID %s must have at least one email",
                    userId
            );
        }

        userEmailRepository.delete(userEmail);
    }
}