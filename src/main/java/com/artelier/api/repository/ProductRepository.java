package com.artelier.api.repository;

import com.artelier.api.entity.Product;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findAll(@NonNull Pageable pageable);

    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);

    Optional<Product> findBySlug(String slug);
}
