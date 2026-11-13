package com.artelier.api.repository;

import com.artelier.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for managing {@link User} persistent entities.
 *
 * <p>Provides database query operations for user accounts, supporting lookups and existence checks
 * by username, primary email address, and unique user UUID identifiers with case-insensitive handling.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Checks whether a user account exists with the specified username, ignoring case differences.
     *
     * @param username the username string to check
     * @return {@code true} if a user with the given username exists; {@code false} otherwise
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Retrieves a user account by its username, ignoring case differences.
     *
     * @param username the username string to search for
     * @return an {@link Optional} containing the found {@link User}, or {@link Optional#empty()} if not found
     */
    Optional<User> findByUsernameIgnoreCase(String username);

    /**
     * Retrieves a user account by its primary contact email address, ignoring case differences.
     *
     * @param primaryEmail the primary email address to search for
     * @return an {@link Optional} containing the found {@link User}, or {@link Optional#empty()} if not found
     */
    Optional<User> findByPrimaryEmailIgnoreCase(String primaryEmail);
}