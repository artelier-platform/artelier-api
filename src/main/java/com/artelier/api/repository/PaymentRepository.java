package com.artelier.api.repository;

import com.artelier.api.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    @Query("SELECT p FROM Payment p JOIN FETCH p.order WHERE p.reference = :reference")
    Optional<Payment> findByReference(String id);
}