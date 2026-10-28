package com.artelier.api.service.impl;

import com.artelier.api.dto.request.BannerRequest;
import com.artelier.api.dto.request.ItemBannerRequest;
import com.artelier.api.dto.request.ItemBannerUpdateRequest;
import com.artelier.api.dto.response.BannerResponse;
import com.artelier.api.dto.response.ItemBannerResponse;
import com.artelier.api.entity.Banner;
import com.artelier.api.entity.ItemBanner;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.mapper.BannerMapper;
import com.artelier.api.mapper.ItemBannerMapper;
import com.artelier.api.repository.BannerRepository;
import com.artelier.api.service.BannerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final BannerMapper bannerMapper;
    private final ItemBannerMapper itemBannerMapper;


    @Override
    @Transactional(readOnly = true)
    public List<BannerResponse> getActiveBanners() {
        return bannerRepository.findByIsActiveTrue()
                .stream()
                .map(bannerMapper::toResponse)
                .map(response -> new BannerResponse(
                        response.id(),
                        response.isActive(),
                        response.createdAt(),
                        response.items().stream()
                                .filter(ItemBannerResponse::isActive)
                                .toList()
                ))
                .filter(response -> !response.items().isEmpty())
                .toList();
    }

    @Override
    public void updateBannerStatus(
            UUID bannerId,
            boolean isActive
    ) {
        Banner banner = findBanner(bannerId);
        banner.setActive(isActive);
        bannerRepository.save(banner);

        log.info(
                "Banner {} status changed to {}",
                bannerId,
                isActive
        );
    }

    @Override
    public void updateItemStatus(
            UUID bannerId,
            UUID itemId,
            boolean isActive
    ) {
        Banner banner = findBanner(bannerId);

        ItemBanner item = banner.getItems()
                .stream()
                .filter(candidate -> candidate.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() ->
                        ArtelierException.notFound(
                                "Banner item not found"
                        )
                );

        item.setActive(isActive);
        bannerRepository.save(banner);

        log.info(
                "Banner item {} status changed to {}",
                itemId,
                isActive
        );
    }

    @Override
    public void deleteBanner(UUID bannerId) {
        Banner banner = findBanner(bannerId);

        bannerRepository.delete(banner);

        log.info("Banner {} deleted successfully", bannerId);
    }

    @Override
    public BannerResponse createBanner(BannerRequest request) {
        Banner banner = Banner.create(request);

        List<ItemBanner> items = getOrderedItems(banner);
        normalizeOrder(items);

        Banner savedBanner = bannerRepository.save(banner);

        log.info(
                "Banner created successfully with id {}",
                savedBanner.getId()
        );

        return bannerMapper.toResponse(savedBanner);
    }

    @Override
    public ItemBannerResponse addItem(
            UUID bannerId,
            ItemBannerRequest request
    ) {
        Banner banner = findBanner(bannerId);
        List<ItemBanner> items = getOrderedItems(banner);

        ItemBanner item = ItemBanner.create(request);
        item.setSortOrder(items.size() + 1);

        banner.addItem(item);
        bannerRepository.save(banner);

        log.info(
                "Item added to banner {} at order {}",
                bannerId,
                item.getSortOrder()
        );

        return itemBannerMapper.toResponse(item);
    }

    @Override
    public ItemBannerResponse addItem(
            UUID bannerId,
            ItemBannerRequest request,
            int order
    ) {
        Banner banner = findBanner(bannerId);
        List<ItemBanner> items = getOrderedItems(banner);

        validateInsertionOrder(order, items.size());

        ItemBanner item = ItemBanner.create(request);

        items.add(order - 1, item);
        item.setBanner(banner);

        normalizeOrder(items);
        bannerRepository.save(banner);

        log.info(
                "Item added to banner {} at order {}",
                bannerId,
                order
        );

        return itemBannerMapper.toResponse(item);
    }

    @Override
    public void deleteItem(UUID bannerId, int order) {
        Banner banner = findBanner(bannerId);
        List<ItemBanner> items = getOrderedItems(banner);

        validateOrder(order, items.size());

        ItemBanner item = items.get(order - 1);

        banner.removeItem(item);

        normalizeOrder(items);
        bannerRepository.save(banner);

        log.info(
                "Item deleted from banner {} at order {}",
                bannerId,
                order
        );
    }

    @Override
    public void reorderItem(
            UUID bannerId,
            int oldOrder,
            int newOrder
    ) {
        Banner banner = findBanner(bannerId);
        List<ItemBanner> items = getOrderedItems(banner);

        validateOrder(oldOrder, items.size());
        validateOrder(newOrder, items.size());

        if (oldOrder == newOrder) {
            return;
        }

        ItemBanner item = items.remove(oldOrder - 1);
        items.add(newOrder - 1, item);

        normalizeOrder(items);
        bannerRepository.save(banner);

        log.info(
                "Item reordered in banner {} from {} to {}",
                bannerId,
                oldOrder,
                newOrder
        );
    }

    @Override
    public ItemBannerResponse updateItem(
            UUID bannerId,
            int order,
            ItemBannerUpdateRequest request
    ) {
        Banner banner = findBanner(bannerId);
        List<ItemBanner> items = getOrderedItems(banner);

        validateOrder(order, items.size());

        ItemBanner item = items.get(order - 1);

        item.setTitle(request.title());
        item.setShowTitle(request.showTitle());

        item.setBackgroundImageUrl(request.backgroundImageUrl());
        item.setBackgroundImageCloudinaryId(
                request.backgroundImageCloudinaryId()
        );

        item.setModel3dUrl(request.model3dUrl());
        item.setModel3dPublicId(request.model3dPublicId());

        item.setDescription(request.description());
        item.setShowDescription(request.showDescription());

        item.setButtonLabel(request.buttonLabel());
        item.setButtonUrl(request.buttonUrl());
        item.setShowButton(request.showButton());

        bannerRepository.save(banner);

        log.info(
                "Item updated in banner {} at order {}",
                bannerId,
                order
        );

        return itemBannerMapper.toResponse(item);
    }

    private Banner findBanner(UUID bannerId) {
        return bannerRepository.findById(bannerId)
                .orElseThrow(() ->
                        ArtelierException.notFound("Banner not found")
                );
    }

    private List<ItemBanner> getOrderedItems(Banner banner) {
        List<ItemBanner> items = banner.getItems();

        items.sort(
                Comparator.comparingInt(ItemBanner::getSortOrder)
        );

        return items;
    }

    private void normalizeOrder(List<ItemBanner> items) {
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setSortOrder(i + 1);
        }
    }

    private void validateOrder(int order, int totalItems) {
        if (order < 1 || order > totalItems) {
            throw ArtelierException.badRequest(
                    "Order must be between 1 and " + totalItems
            );
        }
    }

    private void validateInsertionOrder(int order, int totalItems) {
        if (order < 1 || order > totalItems + 1) {
            throw ArtelierException.badRequest(
                    "Insertion order must be between 1 and "
                            + (totalItems + 1)
            );
        }
    }
}