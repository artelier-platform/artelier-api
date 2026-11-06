package com.artelier.api.repository;

import com.artelier.api.entity.UserEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserEmailRepository extends JpaRepository<UserEmail, UUID> {
    boolean existsByEmail(String email);
    long countByUserId(UUID userId);
    List<UserEmail> findByUserId(UUID userId);
    Optional<UserEmail> findByIdAndUserId(UUID id, UUID userId);
    Optional<UserEmail> findByEmail(String email);
}
