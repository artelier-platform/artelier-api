package com.artelier.api.verification.repository;

import com.artelier.api.verification.entity.PhoneVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, UUID> {
    Optional<PhoneVerification> findLatestByUserId(UUID userId);   // la solicitud más reciente
    long countByUserIdAndCreatedAtAfter(UUID userId, Instant since);
}
