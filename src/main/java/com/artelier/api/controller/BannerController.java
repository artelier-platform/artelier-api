package com.artelier.api.controller;

import com.artelier.api.dto.request.BannerRequest;
import com.artelier.api.dto.request.ItemBannerRequest;
import com.artelier.api.dto.request.ItemBannerUpdateRequest;
import com.artelier.api.dto.request.PositionRequest;
import com.artelier.api.dto.request.StatusRequest;
import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.BannerResponse;
import com.artelier.api.dto.response.ItemBannerResponse;
import com.artelier.api.dto.response.UploadedAssetResponse;
import com.artelier.api.service.BannerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    // =========================
    // PUBLIC
    // =========================

    @GetMapping("/banners")
    public ResponseEntity<AppResponse<List<BannerResponse>>> getActiveBanners() {
        return ResponseEntity.ok(AppResponse.success(
                "Banners retrieved",
                bannerService.getActiveBanners()
        ));
    }

    // =========================
    // ADMIN - BANNERS
    // =========================

    @GetMapping("/admin/banners")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppResponse<List<BannerResponse>>> getAllBanners() {
        return ResponseEntity.ok(AppResponse.success(
                "Banners retrieved",
                bannerService.getAllBanners()
        ));
    }

    @PostMapping(
            value = "/admin/banners/uploads",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppResponse<UploadedAssetResponse>> uploadImage(
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AppResponse.success(
                        "Image uploaded",
                        bannerService.uploadImage(file)
                ));
    }

    @PostMapping("/admin/banners")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppResponse<BannerResponse>> createBanner(
            @Valid @RequestBody BannerRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AppResponse.success(
                        "Banner created",
                        bannerService.createBanner(request)
                ));
    }

    @PatchMapping("/admin/banners/{bannerId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateBannerStatus(
            @PathVariable UUID bannerId,
            @Valid @RequestBody StatusRequest request
    ) {
        bannerService.updateBannerStatus(bannerId, request.isActive());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/banners/{bannerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBanner(@PathVariable UUID bannerId) {
        bannerService.deleteBanner(bannerId);
        return ResponseEntity.noContent().build();
    }

    // =========================
    // ADMIN - BANNER ITEMS
    // =========================

    @PostMapping("/admin/banners/{bannerId}/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppResponse<ItemBannerResponse>> addItem(
            @PathVariable UUID bannerId,
            @Valid @RequestBody ItemBannerRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AppResponse.success(
                        "Item added",
                        bannerService.addItem(bannerId, request)
                ));
    }

    // {order} aquí es la posición donde se inserta el item NUEVO (no identifica un item existente)
    @PostMapping("/admin/banners/{bannerId}/items/{order}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppResponse<ItemBannerResponse>> insertItem(
            @PathVariable UUID bannerId,
            @PathVariable int order,
            @Valid @RequestBody ItemBannerRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AppResponse.success(
                        "Item added",
                        bannerService.addItem(bannerId, request, order)
                ));
    }

    @PutMapping("/admin/banners/{bannerId}/items/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppResponse<ItemBannerResponse>> updateItem(
            @PathVariable UUID bannerId,
            @PathVariable UUID itemId,
            @Valid @RequestBody ItemBannerUpdateRequest request
    ) {
        return ResponseEntity.ok(AppResponse.success(
                "Item updated",
                bannerService.updateItem(bannerId, itemId, request)
        ));
    }

    @PatchMapping("/admin/banners/{bannerId}/items/{itemId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateItemStatus(
            @PathVariable UUID bannerId,
            @PathVariable UUID itemId,
            @Valid @RequestBody StatusRequest request
    ) {
        bannerService.updateItemStatus(bannerId, itemId, request.isActive());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/banners/{bannerId}/items/{itemId}/position")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> moveItem(
            @PathVariable UUID bannerId,
            @PathVariable UUID itemId,
            @Valid @RequestBody PositionRequest request
    ) {
        bannerService.moveItem(bannerId, itemId, request.newOrder());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/banners/{bannerId}/items/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteItem(
            @PathVariable UUID bannerId,
            @PathVariable UUID itemId
    ) {
        bannerService.deleteItem(bannerId, itemId);
        return ResponseEntity.noContent().build();
    }
}
