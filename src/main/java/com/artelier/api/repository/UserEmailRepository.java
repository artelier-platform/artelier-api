package com.artelier.api.repository;

import com.artelier.api.entity.UserEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link UserEmail} entities.
 * Handles persistence, uniqueness checks, association lookups, and account email management.
 */
public interface UserEmailRepository extends JpaRepository<UserEmail, UUID> {

    /**
     * Checks whether an email address already exists in the database.
     *
     * @param email the email address to check
     * @return {@code true} if the email is already registered; {@code false} otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Counts the total number of email addresses associated with a specific user account.
     *
     * @param userId the unique identifier of the user
     * @return the count of associated emails
     */
    long countByUserId(UUID userId);

    /**
     * Retrieves all email addresses associated with a specific user.
     *
     * @param userId the unique identifier of the user
     * @return a list of {@link UserEmail} entities belonging to the user
     */
    List<UserEmail> findByUserId(UUID userId);

    /**
     * Retrieves a specific email record by its unique identifier, ensuring it belongs to the specified user.
     *
     * @param id     the unique identifier of the user email record
     * @param userId the unique identifier of the user owning the record
     * @return an {@link Optional} containing the matching {@link UserEmail} if found
     */
    Optional<UserEmail> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Retrieves a user email record by its email address string.
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the matching {@link UserEmail} if found
     */
    Optional<UserEmail> findByEmail(String email);
}