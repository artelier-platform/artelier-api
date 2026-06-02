package com.artelier.api.repository;

import com.artelier.api.entity.Order;
import com.artelier.api.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    List<Order> findByUserEmailOrderByCreatedAtDesc(String email);

    long countByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o " +
            "WHERE o.status = 'PAID' AND o.createdAt >= :startOfMonth")
    BigDecimal sumTotalSalesSince(@Param("startOfMonth") Instant startOfMonth);
}
