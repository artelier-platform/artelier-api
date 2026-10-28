package com.artelier.api.service.impl;

import com.artelier.api.dto.request.BannerRequest;
import com.artelier.api.dto.request.ItemBannerRequest;
import com.artelier.api.dto.request.ItemBannerUpdateRequest;
import com.artelier.api.dto.response.BannerResponse;
import com.artelier.api.dto.response.ItemBannerResponse;
import com.artelier.api.dto.response.UploadedAssetResponse;
import com.artelier.api.entity.Banner;
import com.artelier.api.entity.ItemBanner;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.integration.cloudinary.dto.response.CloudinaryUploadResponse;
import com.artelier.api.integration.cloudinary.service.CloudinaryService;
import com.artelier.api.mapper.BannerMapper;
import com.artelier.api.mapper.ItemBannerMapper;
import com.artelier.api.repository.BannerRepository;
import com.artelier.api.service.BannerService;
import com.artelier.api.util.ImageValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BannerServiceImpl implements BannerService {

    private static final String BANNER_FOLDER = "artelier/banners";

    private final BannerRepository bannerRepository;
    private final BannerMapper bannerMapper;
    private final ItemBannerMapper itemBannerMapper;
    private final CloudinaryService cloudinaryService;

    private record Asset(String publicId, String resourceType) {
    }

    // =========================
    // READ
    // =========================

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponse> getActiveBanners() {
        return bannerRepository.findByIsActiveTrueOrderByCreatedAtAsc()
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
    @Transactional(readOnly = true)
    public List<BannerResponse> getAllBanners() {
        return bannerRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(bannerMapper::toResponse)
                .toList();
    }

    // =========================
    // UPLOAD
    // =========================

    // Sin transacción: no queremos ocupar una conexión del pool (son pocas)
    // mientras Cloudinary responde.
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UploadedAssetResponse uploadImage(MultipartFile file) {
        ImageValidator.validate(file);

        CloudinaryUploadResponse uploaded =
                cloudinaryService.upload(file, BANNER_FOLDER);

        log.info("Banner image uploaded: {}", uploaded.getPublicId());

        return new UploadedAssetResponse(
                uploaded.getSecureUrl(),
                uploaded.getPublicId()
        );
    }

    // =========================
    // BANNERS
    // =========================

    @Override
    public BannerResponse createBanner(BannerRequest request) {
        Banner banner = Banner.create(request);

        normalizeOrder(getOrderedItems(banner));

        Banner savedBanner = bannerRepository.save(banner);

        log.info("Banner created successfully with id {}", savedBanner.getId());

        return bannerMapper.toResponse(savedBanner);
    }

    @Override
    public void updateBannerStatus(UUID bannerId, boolean isActive) {
        Banner banner = findBanner(bannerId);
        banner.setActive(isActive);

        log.info("Banner {} status changed to {}", bannerId, isActive);
    }

    @Override
    public void deleteBanner(UUID bannerId) {
        Banner banner = findBannerForUpdate(bannerId);

        List<Asset> assets = banner.getItems().stream()
                .flatMap(item -> assetsOf(item).stream())
                .toList();

        bannerRepository.delete(banner);
        deleteAssetsAfterCommit(assets);

        log.info("Banner {} deleted successfully", bannerId);
    }

    // =========================
    // ITEMS
    // =========================

    @Override
    public ItemBannerResponse addItem(UUID bannerId, ItemBannerRequest request) {
        Banner banner = findBannerForUpdate(bannerId);
        List<ItemBanner> items = getOrderedItems(banner);

        ItemBanner item = ItemBanner.create(request);
        item.setSortOrder(items.size() + 1);

        banner.addItem(item);
        bannerRepository.flush(); // asigna el id del item antes de responder

        log.info("Item {} added to banner {} at order {}",
                item.getId(), bannerId, item.getSortOrder());

        return itemBannerMapper.toResponse(item);
    }

    @Override
    public ItemBannerResponse addItem(UUID bannerId, ItemBannerRequest request, int order) {
        Banner banner = findBannerForUpdate(bannerId);
        List<ItemBanner> items = getOrderedItems(banner);

        validateInsertionOrder(order, items.size());

        ItemBanner item = ItemBanner.create(request);

        items.add(order - 1, item);
        item.setBanner(banner);

        normalizeOrder(items);
        bannerRepository.flush();

        log.info("Item {} inserted in banner {} at order {}",
                item.getId(), bannerId, order);

        return itemBannerMapper.toResponse(item);
    }

    @Override
    public ItemBannerResponse updateItem(
            UUID bannerId,
            UUID itemId,
            ItemBannerUpdateRequest request
    ) {
        Banner banner = findBanner(bannerId);
        ItemBanner item = findItem(banner.getItems(), itemId);

        List<Asset> before = assetsOf(item);

        item.setTitle(request.title());
        item.setShowTitle(request.showTitle());

        item.setBackgroundImageUrl(request.backgroundImageUrl());
        item.setBackgroundImageCloudinaryId(request.backgroundImageCloudinaryId());

        item.setModel3dUrl(request.model3dUrl());
        item.setModel3dPublicId(request.model3dPublicId());

        item.setDescription(request.description());
        item.setShowDescription(request.showDescription());

        item.setButtonLabel(request.buttonLabel());
        item.setButtonUrl(request.buttonUrl());
        item.setShowButton(request.showButton());

        // Lo que tenía antes y ya no usa → se borra de Cloudinary tras el commit
        List<Asset> after = assetsOf(item);
        deleteAssetsAfterCommit(
                before.stream().filter(asset -> !after.contains(asset)).toList()
        );

        log.info("Item {} updated in banner {}", itemId, bannerId);

        return itemBannerMapper.toResponse(item);
    }

    @Override
    public void updateItemStatus(UUID bannerId, UUID itemId, boolean isActive) {
        Banner banner = findBanner(bannerId);
        ItemBanner item = findItem(banner.getItems(), itemId);

        item.setActive(isActive);

        log.info("Banner item {} status changed to {}", itemId, isActive);
    }

    @Override
    public void moveItem(UUID bannerId, UUID itemId, int newOrder) {
        Banner banner = findBannerForUpdate(bannerId);
        List<ItemBanner> items = getOrderedItems(banner);

        ItemBanner item = findItem(items, itemId);
        validateOrder(newOrder, items.size());

        int currentOrder = items.indexOf(item) + 1;
        if (currentOrder == newOrder) {
            return;
        }

        items.remove(item);
        items.add(newOrder - 1, item);

        normalizeOrder(items);

        log.info("Item {} moved in banner {} from {} to {}",
                itemId, bannerId, currentOrder, newOrder);
    }

    @Override
    public void deleteItem(UUID bannerId, UUID itemId) {
        Banner banner = findBannerForUpdate(bannerId);
        List<ItemBanner> items = getOrderedItems(banner);

        ItemBanner item = findItem(items, itemId);
        List<Asset> assets = assetsOf(item);

        banner.removeItem(item);
        normalizeOrder(items);

        deleteAssetsAfterCommit(assets);

        log.info("Item {} deleted from banner {}", itemId, bannerId);
    }

    // =========================
    // HELPERS
    // =========================

    private Banner findBanner(UUID bannerId) {
        return bannerRepository.findById(bannerId)
                .orElseThrow(() -> ArtelierException.notFound("Banner not found"));
    }

    private Banner findBannerForUpdate(UUID bannerId) {
        return bannerRepository.findByIdForUpdate(bannerId)
                .orElseThrow(() -> ArtelierException.notFound("Banner not found"));
    }

    private ItemBanner findItem(List<ItemBanner> items, UUID itemId) {
        return items.stream()
                .filter(candidate -> Objects.equals(candidate.getId(), itemId))
                .findFirst()
                .orElseThrow(() -> ArtelierException.notFound("Banner item not found"));
    }

    private List<ItemBanner> getOrderedItems(Banner banner) {
        List<ItemBanner> items = banner.getItems();
        items.sort(Comparator.comparingInt(ItemBanner::getSortOrder));
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
                    "Insertion order must be between 1 and " + (totalItems + 1)
            );
        }
    }

    private List<Asset> assetsOf(ItemBanner item) {
        List<Asset> assets = new ArrayList<>();
        if (item.getBackgroundImageCloudinaryId() != null) {
            assets.add(new Asset(
                    item.getBackgroundImageCloudinaryId(),
                    CloudinaryService.RESOURCE_IMAGE));
        }
        if (item.getModel3dPublicId() != null) {
            assets.add(new Asset(
                    item.getModel3dPublicId(),
                    CloudinaryService.RESOURCE_RAW));
        }
        return assets;
    }

    // Borra en Cloudinary solo si la transacción de BD terminó bien (si hace rollback,
    // la imagen sigue siendo usada). Además solo toca archivos de la carpeta de banners:
    // el publicId llega del cliente y no debe poder apuntar a imágenes de productos.
    private void deleteAssetsAfterCommit(List<Asset> assets) {
        List<Asset> safe = assets.stream()
                .filter(asset -> asset.publicId().startsWith(BANNER_FOLDER + "/"))
                .toList();

        if (safe.isEmpty()) {
            return;
        }

        Runnable task = () -> safe.forEach(asset ->
                cloudinaryService.delete(asset.publicId(), asset.resourceType()));

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            task.run();
                        }
                    });
        } else {
            task.run();
        }
    }
}
