package com.artelier.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent entity representing an email address associated with a user account.
 *
 * <p>Tracks email verification status, primary designation, verification timestamps,
 * and ownership mapping to support multi-email management and authentication workflows.</p>
 */
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

    /**
     * Unique identifier for the user email record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The email address string associated with the user account.
     */
    @NotBlank
    @Email
    @Size(max = 254)
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    /**
     * The user account that owns this email address.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    /**
     * Flag indicating whether this email address is designated as the primary contact channel.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean isPrimary = false;

    /**
     * Flag indicating whether this email address has been successfully verified.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean isVerified = false;

    /**
     * Timestamp indicating when the email address was verified, if applicable.
     */
    private Instant verifiedAt;

    /**
     * Timestamp indicating when the email record was created.
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Lifecycle callback executed before persisting a new entity,
     * automatically populating the creation timestamp.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    /**
     * Checks whether this email address is eligible to be designated as the primary contact channel.
     *
     * @return {@code true} if the email is verified; {@code false} otherwise
     */
    public boolean canBePrimary() {
        return isVerified;
    }
}