package com.artelier.api.repository;

import com.artelier.api.dto.projection.TopProductProjection;
import com.artelier.api.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    @Query("SELECT oi.product.id AS id, oi.product.name AS name, SUM(oi.quantity) AS totalSold " +
            "FROM OrderItem oi " +
            "GROUP BY oi.product.id, oi.product.name " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<TopProductProjection> findTopSellingProducts(Pageable pageable);
}