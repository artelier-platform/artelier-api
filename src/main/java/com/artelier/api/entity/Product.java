package com.artelier.api.entity;

import com.artelier.api.dto.request.ProductRequest;
import com.artelier.api.enums.StockType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted_at IS NULL")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    private String description;

    @Column(columnDefinition = "TEXT")
    private String story;

    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_type", nullable = false)
    private StockType stockType;

    private Integer stockQuantity;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isCustomOrder = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;


    public static Product create(
            ProductRequest request,
            Category category,
            String slug
    ) {
        return Product.builder()
                .name(request.name())
                .slug(slug)
                .description(request.description())
                .story(request.story())
                .price(request.price())
                .stockType(request.stockType())
                .stockQuantity(request.stockQuantity())
                .isCustomOrder(request.isCustomOrder())
                .isActive(request.isActive())
                .category(category)
                .build();
    }
}