package com.artelier.api.banner.service;

import com.artelier.api.banner.dto.BannerRequest;
import com.artelier.api.banner.dto.ItemBannerRequest;
import com.artelier.api.banner.dto.ItemBannerUpdateRequest;
import com.artelier.api.banner.dto.BannerResponse;
import com.artelier.api.banner.dto.ItemBannerResponse;
import com.artelier.api.catalog.dto.UploadedAssetResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface BannerService {

    List<BannerResponse> getActiveBanners();

    List<BannerResponse> getAllBanners();

    UploadedAssetResponse uploadImage(MultipartFile file);

    BannerResponse createBanner(BannerRequest request);

    void updateBannerStatus(UUID bannerId, boolean isActive);

    void deleteBanner(UUID bannerId);

    ItemBannerResponse addItem(UUID bannerId, ItemBannerRequest request);

    ItemBannerResponse addItem(UUID bannerId, ItemBannerRequest request, int order);

    ItemBannerResponse updateItem(UUID bannerId, UUID itemId, ItemBannerUpdateRequest request);

    void updateItemStatus(UUID bannerId, UUID itemId, boolean isActive);

    void moveItem(UUID bannerId, UUID itemId, int newOrder);

    void deleteItem(UUID bannerId, UUID itemId);
}
