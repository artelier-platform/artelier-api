package com.artelier.api.entity;

import com.artelier.api.dto.request.ItemBannerRequest;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "banner_items",
        indexes = {
                @Index(
                        name = "idx_banner_items_order",
                        columnList = "banner_id, sort_order"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banner_id", nullable = false)
    private Banner banner;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 1;

    @Column(length = 120)
    private String title;

    @Builder.Default
    @Column(name = "show_title", nullable = false)
    private boolean showTitle = true;

    @Column(name = "background_image_url", length = 2048)
    private String backgroundImageUrl;

    @Column(name = "background_image_cloudinary_id")
    private String backgroundImageCloudinaryId;

    @Column(name = "model_3d_url", length = 2048)
    private String model3dUrl;

    @Column(name = "model_3d_public_id")
    private String model3dPublicId;

    @Column(length = 300)
    private String description;

    @Builder.Default
    @Column(name = "show_description", nullable = false)
    private boolean showDescription = false;

    @Column(name = "button_label", length = 40)
    private String buttonLabel;

    @Column(name = "button_url", length = 500)
    private String buttonUrl;

    @Builder.Default
    @Column(name = "show_button", nullable = false)
    private boolean showButton = false;

    public static ItemBanner create(ItemBannerRequest request) {
        return ItemBanner.builder()
                .sortOrder(request.sortOrder())
                .title(request.title())
                .showTitle(request.showTitle())
                .backgroundImageUrl(request.backgroundImageUrl())
                .backgroundImageCloudinaryId(
                        request.backgroundImageCloudinaryId()
                )
                .model3dUrl(request.model3dUrl())
                .model3dPublicId(request.model3dPublicId())
                .description(request.description())
                .showDescription(request.showDescription())
                .buttonLabel(request.buttonLabel())
                .buttonUrl(request.buttonUrl())
                .showButton(request.showButton())
                .build();
    }
}