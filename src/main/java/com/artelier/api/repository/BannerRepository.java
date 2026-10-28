package com.artelier.api.repository;

import com.artelier.api.entity.Banner;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BannerRepository extends JpaRepository<Banner, UUID> {

    @EntityGraph(attributePaths = "items")
    List<Banner> findByIsActiveTrueOrderByCreatedAtAsc();

    @EntityGraph(attributePaths = "items")
    List<Banner> findAllByOrderByCreatedAtDesc();

    // Bloquea la fila del banner mientras se reordena / inserta / borra un item,
    // para que dos admins no pisen el sort_order del otro.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Banner b where b.id = :id")
    Optional<Banner> findByIdForUpdate(@Param("id") UUID id);
}
