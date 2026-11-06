package com.artelier.api.entity;

import com.artelier.api.enums.SessionRevocationReason;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "user_sessions",
        indexes = {
                @Index(name = "idx_user_sessions_user_id", columnList = "user_id"),
                @Index(name = "idx_user_sessions_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @NotBlank
    @Column(nullable = false, unique = true, length = 255)
    private String refreshTokenHash;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant lastUsedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private SessionRevocationReason revocationReason;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public boolean isExpired() {
        return !Instant.now().isBefore(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }

    public void revoke(SessionRevocationReason reason) {
        if (revokedAt == null) {
            revokedAt = Instant.now();
            revocationReason = reason;
        }
    }

    public void updateLastUsed() {
        lastUsedAt = Instant.now();
    }
}