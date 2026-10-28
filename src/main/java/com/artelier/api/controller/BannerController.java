package com.artelier.api.controller;

import com.artelier.api.dto.request.BannerRequest;
import com.artelier.api.dto.request.ItemBannerRequest;
import com.artelier.api.dto.request.ItemBannerUpdateRequest;
import com.artelier.api.dto.request.StatusRequest;
import com.artelier.api.dto.response.BannerResponse;
import com.artelier.api.dto.response.ItemBannerResponse;
import com.artelier.api.service.BannerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    // =========================
    // PUBLIC
    // =========================

    @GetMapping("/banner")
    public ResponseEntity<List<BannerResponse>> getActiveBanners() {
        return ResponseEntity.ok(
                bannerService.getActiveBanners()
        );
    }

    // =========================
    // ADMIN - BANNERS
    // =========================

    @PostMapping("/admin/banners")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerResponse> createBanner(
            @Valid @RequestBody BannerRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bannerService.createBanner(request));
    }

    @PatchMapping("/admin/banners/{bannerId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateBannerStatus(
            @PathVariable UUID bannerId,
            @Valid @RequestBody StatusRequest request
    ) {
        bannerService.updateBannerStatus(
                bannerId,
                request.isActive()
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/banners/{bannerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBanner(
            @PathVariable UUID bannerId
    ) {
        bannerService.deleteBanner(bannerId);

        return ResponseEntity.noContent().build();
    }

    // =========================
    // ADMIN - BANNER ITEMS
    // =========================

    @PostMapping("/admin/banners/{bannerId}/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemBannerResponse> addItem(
            @PathVariable UUID bannerId,
            @Valid @RequestBody ItemBannerRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bannerService.addItem(bannerId, request));
    }

    @PostMapping("/admin/banners/{bannerId}/items/{order}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemBannerResponse> insertItem(
            @PathVariable UUID bannerId,
            @PathVariable int order,
            @Valid @RequestBody ItemBannerRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        bannerService.addItem(
                                bannerId,
                                request,
                                order
                        )
                );
    }

    @PutMapping("/admin/banners/{bannerId}/items/{order}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemBannerResponse> updateItem(
            @PathVariable UUID bannerId,
            @PathVariable int order,
            @Valid @RequestBody ItemBannerUpdateRequest request
    ) {
        return ResponseEntity.ok(
                bannerService.updateItem(
                        bannerId,
                        order,
                        request
                )
        );
    }

    @PatchMapping(
            "/admin/banners/{bannerId}/items/{itemId}/status"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateItemStatus(
            @PathVariable UUID bannerId,
            @PathVariable UUID itemId,
            @Valid @RequestBody StatusRequest request
    ) {
        bannerService.updateItemStatus(
                bannerId,
                itemId,
                request.isActive()
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/banners/{bannerId}/items/{order}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteItem(
            @PathVariable UUID bannerId,
            @PathVariable int order
    ) {
        bannerService.deleteItem(bannerId, order);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/banners/{bannerId}/items/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reorderItem(
            @PathVariable UUID bannerId,
            @RequestParam int oldOrder,
            @RequestParam int newOrder
    ) {
        bannerService.reorderItem(
                bannerId,
                oldOrder,
                newOrder
        );

        return ResponseEntity.noContent().build();
    }
}