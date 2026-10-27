package com.artelier.api.controller;

import com.artelier.api.dto.request.BannerRequest;
import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.BannerResponse;
import com.artelier.api.service.BannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping("/banners")
    public ResponseEntity<AppResponse<List<BannerResponse>>> listActive() {
        return ResponseEntity.ok(AppResponse.success("Banners retrieved", bannerService.listActive()));
    }

    @GetMapping("/admin/banners")
    public ResponseEntity<AppResponse<List<BannerResponse>>> listAll() {
        return ResponseEntity.ok(AppResponse.success("Banners retrieved", bannerService.listAll()));
    }

    @PostMapping(value = "/banners", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AppResponse<BannerResponse>> create(
            @RequestPart("data") @Valid BannerRequest data,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AppResponse.success("Banner created", bannerService.create(data, image)));
    }

    @PutMapping(value = "/banners/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AppResponse<BannerResponse>> update(
            @PathVariable UUID id,
            @RequestPart("data") @Valid BannerRequest data,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.ok(AppResponse.success("Banner updated", bannerService.update(id, data, image)));
    }

    @DeleteMapping("/banners/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        bannerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/banners/{id}/toggle")
    public ResponseEntity<AppResponse<BannerResponse>> toggle(@PathVariable UUID id) {
        return ResponseEntity.ok(AppResponse.success("Banner toggled", bannerService.toggle(id)));
    }
}
