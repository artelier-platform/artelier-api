package com.artelier.api.repository;

import com.artelier.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByUsernameIgnoreCase(String username);
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByPrimaryEmailIgnoreCase(String primaryEmail);
    Optional<User> findByUserUUID(UUID userUUID);
}