package com.artelier.api.entity;

import com.artelier.api.enums.Role;
import com.artelier.api.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_username", columnNames = "username")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 12)
    @Pattern(regexp = "^[\\x21-\\x3F\\x41-\\x7E]{1,12}$")
    @Column(nullable = false, unique = true, length = 12)
    private String username;

    @NotBlank
    @Column(nullable = false)
    private String passwordHash;

    @NotBlank
    @Size(min = 1, max = 58)
    @Column(nullable = false, length = 58)
    private String firstName;

    @NotBlank
    @Size(min = 1, max = 34)
    @Column(nullable = false, length = 34)
    private String lastName;

    @NotBlank
    @Size(max = 4)
    @Column(nullable = false, length = 4)
    private String phoneCountryCode;

    @NotBlank
    @Size(max = 15)
    @Column(nullable = false, length = 15)
    private String phoneNumber;

    private Instant phoneVerifiedAt;

    @Size(max = 500)
    @Column(length = 500)
    private String profileImageUrl;

    @Size(max = 255)
    @Column(length = 255)
    private String profileImagePublicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<UserEmail> emails = new ArrayList<>();

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<UserSession> sessions = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isPhoneVerified() {
        return phoneVerifiedAt != null;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public Optional<String> getPrimaryEmail() {
        return emails.stream()
                .filter(UserEmail::isVerified)
                .filter(UserEmail::isPrimary)
                .map(UserEmail::getEmail)
                .findFirst();
    }
}