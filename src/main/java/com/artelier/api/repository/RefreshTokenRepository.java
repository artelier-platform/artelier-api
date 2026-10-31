package com.artelier.api.repository;

import com.artelier.api.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    @Modifying
    @Query("""
    DELETE FROM RefreshToken rt
    WHERE rt.token = :token
    AND rt.expiresAt > :now
""")
    int consumeIfValid(
            @Param("token") String token,
            @Param("now") Instant now
    );

    Optional<RefreshToken> findByToken(String token);
    void deleteByToken(String token);
}