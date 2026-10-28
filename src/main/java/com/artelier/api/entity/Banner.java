package com.artelier.api.entity;

import com.artelier.api.dto.request.BannerRequest;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "banners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(
            mappedBy = "banner",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("sortOrder ASC")
    private List<ItemBanner> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public static Banner create(BannerRequest request) {
        Banner banner = Banner.builder()
                .isActive(request.isActive() == null || request.isActive())
                .build();

        if (request.items() != null) {
            request.items().forEach(itemRequest ->
                    banner.addItem(ItemBanner.create(itemRequest))
            );
        }

        return banner;
    }

    public void addItem(ItemBanner item) {
        items.add(item);
        item.setBanner(this);
    }

    public void removeItem(ItemBanner item) {
        if (items.remove(item)) {
            item.setBanner(null);
        }
    }
}