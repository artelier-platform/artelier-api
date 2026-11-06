package com.artelier.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "phone_verifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneVerification {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 4)   private String phoneCountryCode;
    @Column(nullable = false, length = 15)  private String phoneNumber;
    @Column(nullable = false, length = 255) private String codeHash;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant expiresAt;
    private Instant consumedAt;
    @Column(nullable = false) private int attempts;
    @PrePersist protected void onCreate() { createdAt = Instant.now(); }
}