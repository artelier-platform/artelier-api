package com.artelier.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "user_emails",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_emails_email",
                        columnNames = "email"
                )
        },
        indexes = {
                @Index(name = "idx_user_emails_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Email
    @Size(max = 254)
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPrimary = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean isVerified = false;

    private Instant verifiedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public boolean canBePrimary() {
        return isVerified;
    }
}