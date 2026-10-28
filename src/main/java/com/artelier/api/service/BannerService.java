package com.artelier.api.service;

import com.artelier.api.dto.request.BannerRequest;
import com.artelier.api.dto.request.ItemBannerRequest;
import com.artelier.api.dto.request.ItemBannerUpdateRequest;
import com.artelier.api.dto.response.BannerResponse;
import com.artelier.api.dto.response.ItemBannerResponse;

import java.util.List;
import java.util.UUID;

public interface BannerService {

    List<BannerResponse> getActiveBanners();

    void updateBannerStatus(UUID bannerId, boolean isActive);

    void updateItemStatus(
            UUID bannerId,
            UUID itemId,
            boolean isActive
    );

    BannerResponse createBanner(BannerRequest request);

    ItemBannerResponse addItem(UUID bannerId, ItemBannerRequest request);

    ItemBannerResponse addItem(
            UUID bannerId,
            ItemBannerRequest request,
            int order
    );

    void deleteItem(UUID bannerId, int order);

    void reorderItem(UUID bannerId, int oldOrder, int newOrder);

    ItemBannerResponse updateItem(
            UUID bannerId,
            int order,
            ItemBannerUpdateRequest request
    );

    void deleteBanner(UUID bannerId);
}