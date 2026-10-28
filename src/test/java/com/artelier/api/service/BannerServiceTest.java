package com.artelier.api.service;

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
import com.artelier.api.service.impl.BannerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class BannerServiceTest {

    @Mock private BannerRepository bannerRepository;
    @Mock private BannerMapper bannerMapper;
    @Mock private ItemBannerMapper itemBannerMapper;
    @Mock private CloudinaryService cloudinaryService;

    @InjectMocks
    private BannerServiceImpl service;

    // =========================
    // HELPERS
    // =========================

    private ItemBanner buildItem(int order, String title) {
        return buildItem(
                order, title,
                "artelier/banners/" + title,
                null
        );
    }

    private ItemBanner buildItem(int order, String title, String imageId, String modelId) {
        return ItemBanner.builder()
                .id(UUID.randomUUID())
                .sortOrder(order)
                .title(title)
                .backgroundImageUrl("https://res.cloudinary.com/artelier/image/upload/" + title + ".jpg")
                .backgroundImageCloudinaryId(imageId)
                .model3dPublicId(modelId)
                .build();
    }

    /** Builds a banner and links every item to it through the aggregate helper. */
    private Banner buildBanner(ItemBanner... items) {
        Banner banner = Banner.builder()
                .id(UUID.randomUUID())
                .build();

        for (ItemBanner item : items) {
            banner.addItem(item);
        }

        return banner;
    }

    private ItemBannerRequest itemRequest(int sortOrder, String title) {
        return new ItemBannerRequest(
                sortOrder, title, true,
                "https://res.cloudinary.com/artelier/image/upload/" + title + ".jpg",
                "artelier/banners/" + title,
                null, null,
                "desc", false,
                null, null, false
        );
    }

    private ItemBannerUpdateRequest updateRequest() {
        return updateRequest("artelier/banners/new", "artelier/banners/model");
    }

    private ItemBannerUpdateRequest updateRequest(String imageId, String modelId) {
        return new ItemBannerUpdateRequest(
                "New title", true,
                "https://res.cloudinary.com/artelier/image/upload/new.jpg",
                imageId,
                "https://res.cloudinary.com/artelier/raw/upload/model.glb",
                modelId,
                "New description", true,
                "Shop now", "https://artelier.com/shop", true
        );
    }

    private ItemBannerResponse itemResponse(boolean active, int order) {
        return new ItemBannerResponse(
                UUID.randomUUID(), active, order, "Item " + order, true,
                "https://res.cloudinary.com/artelier/image/upload/bg.jpg", null,
                null, null,
                "desc", false,
                null, null, false
        );
    }

    private List<String> titles(Banner banner) {
        return banner.getItems().stream().map(ItemBanner::getTitle).toList();
    }

    private List<Integer> orders(Banner banner) {
        return banner.getItems().stream().map(ItemBanner::getSortOrder).toList();
    }

    private void stubBannerForUpdate(Banner banner) {
        when(bannerRepository.findByIdForUpdate(banner.getId())).thenReturn(Optional.of(banner));
    }

    private void stubBanner(Banner banner) {
        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));
    }

    private MockMultipartFile pngFile() {
        byte[] png = {
                (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D
        };
        return new MockMultipartFile("file", "banner.png", "image/png", png);
    }

    // =========================
    // getActiveBanners
    // =========================

    @Test
    void shouldReturnOnlyActiveItemsOfActiveBanners() {
        Banner banner = buildBanner();
        ItemBannerResponse active = itemResponse(true, 1);
        ItemBannerResponse inactive = itemResponse(false, 2);
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now();

        when(bannerRepository.findByIsActiveTrueOrderByCreatedAtAsc()).thenReturn(List.of(banner));
        when(bannerMapper.toResponse(banner))
                .thenReturn(new BannerResponse(id, true, createdAt, List.of(active, inactive)));

        List<BannerResponse> result = service.getActiveBanners();

        assertEquals(1, result.size());
        assertEquals(id, result.getFirst().id());
        assertEquals(createdAt, result.getFirst().createdAt());
        assertEquals(List.of(active), result.getFirst().items());
    }

    @Test
    void shouldExcludeBannersWithoutActiveItems() {
        Banner banner = buildBanner();

        when(bannerRepository.findByIsActiveTrueOrderByCreatedAtAsc()).thenReturn(List.of(banner));
        when(bannerMapper.toResponse(banner))
                .thenReturn(new BannerResponse(
                        UUID.randomUUID(), true, Instant.now(),
                        List.of(itemResponse(false, 1), itemResponse(false, 2))
                ));

        List<BannerResponse> result = service.getActiveBanners();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWhenThereAreNoActiveBanners() {
        when(bannerRepository.findByIsActiveTrueOrderByCreatedAtAsc()).thenReturn(List.of());

        List<BannerResponse> result = service.getActiveBanners();

        assertTrue(result.isEmpty());
        verify(bannerMapper, never()).toResponse(any());
    }

    // =========================
    // getAllBanners
    // =========================

    @Test
    void shouldReturnAllBannersIncludingInactiveOnes() {
        Banner active = buildBanner(buildItem(1, "A"));
        Banner inactive = buildBanner(buildItem(1, "B"));
        inactive.setActive(false);

        BannerResponse activeResponse = new BannerResponse(
                active.getId(), true, Instant.now(), List.of(itemResponse(true, 1)));
        BannerResponse inactiveResponse = new BannerResponse(
                inactive.getId(), false, Instant.now(), List.of(itemResponse(false, 1)));

        when(bannerRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(active, inactive));
        when(bannerMapper.toResponse(active)).thenReturn(activeResponse);
        when(bannerMapper.toResponse(inactive)).thenReturn(inactiveResponse);

        List<BannerResponse> result = service.getAllBanners();

        // Unlike the public listing, nothing is filtered out
        assertEquals(List.of(activeResponse, inactiveResponse), result);
    }

    @Test
    void shouldReturnEmptyListWhenThereAreNoBanners() {
        when(bannerRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        assertTrue(service.getAllBanners().isEmpty());
    }

    // =========================
    // uploadImage
    // =========================

    @Test
    void shouldUploadImageToTheBannersFolder() {
        MockMultipartFile file = pngFile();
        CloudinaryUploadResponse uploaded = mock(CloudinaryUploadResponse.class);

        when(uploaded.getSecureUrl()).thenReturn("https://res.cloudinary.com/artelier/image/upload/banners/x.png");
        when(uploaded.getPublicId()).thenReturn("artelier/banners/x");
        when(cloudinaryService.upload(file, "artelier/banners")).thenReturn(uploaded);

        UploadedAssetResponse result = service.uploadImage(file);

        assertEquals("https://res.cloudinary.com/artelier/image/upload/banners/x.png", result.url());
        assertEquals("artelier/banners/x", result.publicId());
        verify(cloudinaryService).upload(file, "artelier/banners");
    }

    @Test
    void shouldRejectUploadWhenFileIsEmpty() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThrows(ArtelierException.class, () -> service.uploadImage(empty));

        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void shouldRejectUploadWhenFileIsNotAnImage() {
        MockMultipartFile text = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "hello world".getBytes());

        assertThrows(ArtelierException.class, () -> service.uploadImage(text));

        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void shouldRejectUploadWhenContentTypeLiesAboutTheBytes() {
        // Says image/png but the bytes are plain text
        MockMultipartFile fake = new MockMultipartFile(
                "file", "fake.png", "image/png", "this is not a png at all".getBytes());

        assertThrows(ArtelierException.class, () -> service.uploadImage(fake));

        verifyNoInteractions(cloudinaryService);
    }

    // =========================
    // updateBannerStatus
    // =========================

    @Test
    void shouldUpdateBannerStatus() {
        Banner banner = buildBanner();
        assertTrue(banner.isActive());

        stubBanner(banner);

        service.updateBannerStatus(banner.getId(), false);

        assertFalse(banner.isActive());
    }

    @Test
    void shouldThrowIfBannerNotFoundOnUpdateStatus() {
        when(bannerRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.updateBannerStatus(UUID.randomUUID(), true));
    }

    // =========================
    // updateItemStatus
    // =========================

    @Test
    void shouldUpdateItemStatus() {
        ItemBanner item = buildItem(1, "A");
        Banner banner = buildBanner(item);

        stubBanner(banner);

        service.updateItemStatus(banner.getId(), item.getId(), false);

        assertFalse(item.isActive());
    }

    @Test
    void shouldThrowIfItemNotFoundOnUpdateItemStatus() {
        ItemBanner item = buildItem(1, "A");
        Banner banner = buildBanner(item);

        stubBanner(banner);

        assertThrows(ArtelierException.class,
                () -> service.updateItemStatus(banner.getId(), UUID.randomUUID(), false));

        assertTrue(item.isActive());
    }

    @Test
    void shouldThrowIfBannerNotFoundOnUpdateItemStatus() {
        when(bannerRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.updateItemStatus(UUID.randomUUID(), UUID.randomUUID(), true));
    }

    // =========================
    // deleteBanner
    // =========================

    @Test
    void shouldDeleteBanner() {
        Banner banner = buildBanner();

        stubBannerForUpdate(banner);

        service.deleteBanner(banner.getId());

        verify(bannerRepository).delete(banner);
    }

    @Test
    void shouldDeleteTheCloudinaryAssetsOfEveryItemWhenDeletingABanner() {
        Banner banner = buildBanner(
                buildItem(1, "A", "artelier/banners/a", null),
                buildItem(2, "B", "artelier/banners/b", "artelier/banners/b-model")
        );

        stubBannerForUpdate(banner);

        service.deleteBanner(banner.getId());

        verify(bannerRepository).delete(banner);
        verify(cloudinaryService).delete("artelier/banners/a", CloudinaryService.RESOURCE_IMAGE);
        verify(cloudinaryService).delete("artelier/banners/b", CloudinaryService.RESOURCE_IMAGE);
        verify(cloudinaryService).delete("artelier/banners/b-model", CloudinaryService.RESOURCE_RAW);
        verifyNoMoreInteractions(cloudinaryService);
    }

    @Test
    void shouldNotDeleteAssetsOutsideTheBannersFolder() {
        // A publicId that points to a product image must never be deleted from here
        Banner banner = buildBanner(
                buildItem(1, "A", "artelier/products/shoe", "other-folder/model")
        );

        stubBannerForUpdate(banner);

        service.deleteBanner(banner.getId());

        verify(bannerRepository).delete(banner);
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void shouldDeleteCloudinaryAssetsOnlyAfterCommit() {
        Banner banner = buildBanner(buildItem(1, "A", "artelier/banners/a", null));

        stubBannerForUpdate(banner);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.deleteBanner(banner.getId());

            // Still inside the transaction: nothing may be deleted yet
            verifyNoInteractions(cloudinaryService);

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            verify(cloudinaryService).delete("artelier/banners/a", CloudinaryService.RESOURCE_IMAGE);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldKeepCloudinaryAssetsWhenTheTransactionDoesNotCommit() {
        Banner banner = buildBanner(buildItem(1, "A", "artelier/banners/a", null));

        stubBannerForUpdate(banner);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.deleteBanner(banner.getId());
            // Rollback: afterCommit is never invoked
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void shouldThrowIfBannerNotFoundOnDelete() {
        when(bannerRepository.findByIdForUpdate(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.deleteBanner(UUID.randomUUID()));

        verify(bannerRepository, never()).delete(any());
        verifyNoInteractions(cloudinaryService);
    }

    // =========================
    // createBanner
    // =========================

    @Test
    void shouldCreateBannerAndNormalizeItemOrder() {
        // Intentionally unordered and non-consecutive sort orders
        BannerRequest request = new BannerRequest(
                true,
                List.of(itemRequest(5, "B"), itemRequest(2, "A"))
        );
        BannerResponse response = new BannerResponse(UUID.randomUUID(), true, Instant.now(), List.of());

        when(bannerRepository.save(any(Banner.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bannerMapper.toResponse(any(Banner.class))).thenReturn(response);

        BannerResponse result = service.createBanner(request);

        assertSame(response, result);

        ArgumentCaptor<Banner> captor = ArgumentCaptor.forClass(Banner.class);
        verify(bannerRepository).save(captor.capture());

        Banner saved = captor.getValue();
        assertEquals(List.of("A", "B"), titles(saved));
        assertEquals(List.of(1, 2), orders(saved));
        saved.getItems().forEach(item -> assertSame(saved, item.getBanner()));
    }

    @Test
    void shouldCreateActiveBannerWhenIsActiveIsNull() {
        BannerRequest request = new BannerRequest(null, List.of(itemRequest(1, "A")));

        when(bannerRepository.save(any(Banner.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bannerMapper.toResponse(any(Banner.class)))
                .thenReturn(new BannerResponse(UUID.randomUUID(), true, Instant.now(), List.of()));

        service.createBanner(request);

        ArgumentCaptor<Banner> captor = ArgumentCaptor.forClass(Banner.class);
        verify(bannerRepository).save(captor.capture());
        assertTrue(captor.getValue().isActive());
    }

    @Test
    void shouldCreateInactiveBannerWhenIsActiveIsFalse() {
        BannerRequest request = new BannerRequest(false, List.of(itemRequest(1, "A")));

        when(bannerRepository.save(any(Banner.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bannerMapper.toResponse(any(Banner.class)))
                .thenReturn(new BannerResponse(UUID.randomUUID(), false, Instant.now(), List.of()));

        service.createBanner(request);

        ArgumentCaptor<Banner> captor = ArgumentCaptor.forClass(Banner.class);
        verify(bannerRepository).save(captor.capture());
        assertFalse(captor.getValue().isActive());
    }

    // =========================
    // addItem (append)
    // =========================

    @Test
    void shouldAddItemAtTheEnd() {
        Banner banner = buildBanner(buildItem(1, "A"), buildItem(2, "B"));
        ItemBannerResponse response = itemResponse(true, 3);

        stubBannerForUpdate(banner);
        when(itemBannerMapper.toResponse(any(ItemBanner.class))).thenReturn(response);

        ItemBannerResponse result = service.addItem(banner.getId(), itemRequest(99, "C"));

        assertSame(response, result);
        assertEquals(3, banner.getItems().size());

        // The requested sortOrder is ignored: the item is always appended
        ArgumentCaptor<ItemBanner> captor = ArgumentCaptor.forClass(ItemBanner.class);
        verify(itemBannerMapper).toResponse(captor.capture());
        assertEquals(3, captor.getValue().getSortOrder());
        assertEquals("C", captor.getValue().getTitle());
        assertSame(banner, captor.getValue().getBanner());

        // flush (not save) so the generated id is available for the response
        verify(bannerRepository).flush();
        verify(bannerRepository, never()).save(any());
    }

    @Test
    void shouldAddFirstItemWithOrderOne() {
        Banner banner = buildBanner();

        stubBannerForUpdate(banner);
        when(itemBannerMapper.toResponse(any(ItemBanner.class))).thenReturn(itemResponse(true, 1));

        service.addItem(banner.getId(), itemRequest(1, "A"));

        assertEquals(List.of(1), orders(banner));
    }

    @Test
    void shouldThrowIfBannerNotFoundOnAddItem() {
        when(bannerRepository.findByIdForUpdate(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.addItem(UUID.randomUUID(), itemRequest(1, "A")));

        verify(bannerRepository, never()).flush();
    }

    // =========================
    // addItem (insert at order)
    // =========================

    @Test
    void shouldInsertItemAtTheBeginningAndShiftTheRest() {
        Banner banner = buildBanner(buildItem(1, "A"), buildItem(2, "B"));
        ItemBannerResponse response = itemResponse(true, 1);

        stubBannerForUpdate(banner);
        when(itemBannerMapper.toResponse(any(ItemBanner.class))).thenReturn(response);

        ItemBannerResponse result = service.addItem(banner.getId(), itemRequest(1, "NEW"), 1);

        assertSame(response, result);
        assertEquals(List.of("NEW", "A", "B"), titles(banner));
        assertEquals(List.of(1, 2, 3), orders(banner));
        banner.getItems().forEach(item -> assertSame(banner, item.getBanner()));
        verify(bannerRepository).flush();
    }

    @Test
    void shouldInsertItemInTheMiddle() {
        Banner banner = buildBanner(buildItem(1, "A"), buildItem(2, "B"), buildItem(3, "C"));

        stubBannerForUpdate(banner);
        when(itemBannerMapper.toResponse(any(ItemBanner.class))).thenReturn(itemResponse(true, 2));

        service.addItem(banner.getId(), itemRequest(1, "NEW"), 2);

        assertEquals(List.of("A", "NEW", "B", "C"), titles(banner));
        assertEquals(List.of(1, 2, 3, 4), orders(banner));
    }

    @Test
    void shouldInsertItemAtTheEndWhenOrderIsSizePlusOne() {
        Banner banner = buildBanner(buildItem(1, "A"), buildItem(2, "B"));

        stubBannerForUpdate(banner);
        when(itemBannerMapper.toResponse(any(ItemBanner.class))).thenReturn(itemResponse(true, 3));

        service.addItem(banner.getId(), itemRequest(1, "NEW"), 3);

        assertEquals(List.of("A", "B", "NEW"), titles(banner));
        assertEquals(List.of(1, 2, 3), orders(banner));
    }

    @Test
    void shouldThrowIfInsertOrderIsLessThanOne() {
        Banner banner = buildBanner(buildItem(1, "A"));

        stubBannerForUpdate(banner);

        assertThrows(ArtelierException.class,
                () -> service.addItem(banner.getId(), itemRequest(1, "NEW"), 0));

        verify(bannerRepository, never()).flush();
    }

    @Test
    void shouldThrowIfInsertOrderIsGreaterThanSizePlusOne() {
        Banner banner = buildBanner(buildItem(1, "A"));

        stubBannerForUpdate(banner);

        assertThrows(ArtelierException.class,
                () -> service.addItem(banner.getId(), itemRequest(1, "NEW"), 3));

        assertEquals(1, banner.getItems().size());
        verify(bannerRepository, never()).flush();
    }

    @Test
    void shouldThrowIfBannerNotFoundOnInsertItem() {
        when(bannerRepository.findByIdForUpdate(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.addItem(UUID.randomUUID(), itemRequest(1, "A"), 1));

        verify(bannerRepository, never()).flush();
    }

    // =========================
    // deleteItem (by itemId)
    // =========================

    @Test
    void shouldDeleteItemAndNormalizeOrder() {
        ItemBanner a = buildItem(1, "A");
        ItemBanner b = buildItem(2, "B");
        ItemBanner c = buildItem(3, "C");
        Banner banner = buildBanner(a, b, c);

        stubBannerForUpdate(banner);

        service.deleteItem(banner.getId(), b.getId());

        assertEquals(List.of("A", "C"), titles(banner));
        assertEquals(List.of(1, 2), orders(banner));
        assertNull(b.getBanner());
    }

    @Test
    void shouldDeleteTheRightItemEvenIfOthersWereReordered() {
        // The id identifies the item, not its position: a stale position can't hit the wrong one
        ItemBanner a = buildItem(1, "A");
        ItemBanner b = buildItem(2, "B");
        Banner banner = buildBanner(a, b);

        stubBannerForUpdate(banner);

        service.deleteItem(banner.getId(), a.getId());

        assertEquals(List.of("B"), titles(banner));
        assertEquals(List.of(1), orders(banner));
    }

    @Test
    void shouldDeleteTheOnlyItem() {
        ItemBanner only = buildItem(1, "A");
        Banner banner = buildBanner(only);

        stubBannerForUpdate(banner);

        service.deleteItem(banner.getId(), only.getId());

        assertTrue(banner.getItems().isEmpty());
    }

    @Test
    void shouldDeleteTheCloudinaryAssetsOfTheDeletedItem() {
        ItemBanner a = buildItem(1, "A", "artelier/banners/a", "artelier/banners/a-model");
        ItemBanner b = buildItem(2, "B", "artelier/banners/b", null);
        Banner banner = buildBanner(a, b);

        stubBannerForUpdate(banner);

        service.deleteItem(banner.getId(), a.getId());

        verify(cloudinaryService).delete("artelier/banners/a", CloudinaryService.RESOURCE_IMAGE);
        verify(cloudinaryService).delete("artelier/banners/a-model", CloudinaryService.RESOURCE_RAW);
        verifyNoMoreInteractions(cloudinaryService); // B's image is untouched
    }

    @Test
    void shouldThrowIfItemNotFoundOnDeleteItem() {
        Banner banner = buildBanner(buildItem(1, "A"), buildItem(2, "B"));

        stubBannerForUpdate(banner);

        assertThrows(ArtelierException.class,
                () -> service.deleteItem(banner.getId(), UUID.randomUUID()));

        assertEquals(2, banner.getItems().size());
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void shouldThrowIfBannerNotFoundOnDeleteItem() {
        when(bannerRepository.findByIdForUpdate(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.deleteItem(UUID.randomUUID(), UUID.randomUUID()));

        verifyNoInteractions(cloudinaryService);
    }

    // =========================
    // moveItem
    // =========================

    @Test
    void shouldMoveItemForward() {
        ItemBanner a = buildItem(1, "A");
        Banner banner = buildBanner(a, buildItem(2, "B"), buildItem(3, "C"));

        stubBannerForUpdate(banner);

        service.moveItem(banner.getId(), a.getId(), 3);

        assertEquals(List.of("B", "C", "A"), titles(banner));
        assertEquals(List.of(1, 2, 3), orders(banner));
    }

    @Test
    void shouldMoveItemBackward() {
        ItemBanner c = buildItem(3, "C");
        Banner banner = buildBanner(buildItem(1, "A"), buildItem(2, "B"), c);

        stubBannerForUpdate(banner);

        service.moveItem(banner.getId(), c.getId(), 1);

        assertEquals(List.of("C", "A", "B"), titles(banner));
        assertEquals(List.of(1, 2, 3), orders(banner));
    }

    @Test
    void shouldMoveItemToTheMiddle() {
        ItemBanner a = buildItem(1, "A");
        Banner banner = buildBanner(a, buildItem(2, "B"), buildItem(3, "C"), buildItem(4, "D"));

        stubBannerForUpdate(banner);

        service.moveItem(banner.getId(), a.getId(), 3);

        assertEquals(List.of("B", "C", "A", "D"), titles(banner));
        assertEquals(List.of(1, 2, 3, 4), orders(banner));
    }

    @Test
    void shouldDoNothingWhenItemIsAlreadyAtTheRequestedPosition() {
        ItemBanner b = buildItem(2, "B");
        Banner banner = buildBanner(buildItem(1, "A"), b);

        stubBannerForUpdate(banner);

        service.moveItem(banner.getId(), b.getId(), 2);

        assertEquals(List.of("A", "B"), titles(banner));
        assertEquals(List.of(1, 2), orders(banner));
    }

    @Test
    void shouldThrowIfMoveNewOrderIsOutOfRange() {
        ItemBanner a = buildItem(1, "A");
        Banner banner = buildBanner(a, buildItem(2, "B"));

        stubBannerForUpdate(banner);

        assertThrows(ArtelierException.class, () -> service.moveItem(banner.getId(), a.getId(), 0));
        assertThrows(ArtelierException.class, () -> service.moveItem(banner.getId(), a.getId(), 3));

        assertEquals(List.of("A", "B"), titles(banner));
    }

    @Test
    void shouldThrowIfItemNotFoundOnMove() {
        Banner banner = buildBanner(buildItem(1, "A"), buildItem(2, "B"));

        stubBannerForUpdate(banner);

        assertThrows(ArtelierException.class,
                () -> service.moveItem(banner.getId(), UUID.randomUUID(), 1));

        assertEquals(List.of("A", "B"), titles(banner));
    }

    @Test
    void shouldThrowIfBannerNotFoundOnMove() {
        when(bannerRepository.findByIdForUpdate(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.moveItem(UUID.randomUUID(), UUID.randomUUID(), 1));
    }

    // =========================
    // updateItem (by itemId)
    // =========================

    @Test
    void shouldUpdateItemFields() {
        ItemBanner first = buildItem(1, "A");
        ItemBanner second = buildItem(2, "B");
        Banner banner = buildBanner(first, second);
        ItemBannerResponse response = itemResponse(true, 2);

        stubBanner(banner);
        when(itemBannerMapper.toResponse(second)).thenReturn(response);

        ItemBannerResponse result = service.updateItem(banner.getId(), second.getId(), updateRequest());

        assertSame(response, result);

        assertEquals("New title", second.getTitle());
        assertTrue(second.isShowTitle());
        assertEquals("https://res.cloudinary.com/artelier/image/upload/new.jpg", second.getBackgroundImageUrl());
        assertEquals("artelier/banners/new", second.getBackgroundImageCloudinaryId());
        assertEquals("https://res.cloudinary.com/artelier/raw/upload/model.glb", second.getModel3dUrl());
        assertEquals("artelier/banners/model", second.getModel3dPublicId());
        assertEquals("New description", second.getDescription());
        assertTrue(second.isShowDescription());
        assertEquals("Shop now", second.getButtonLabel());
        assertEquals("https://artelier.com/shop", second.getButtonUrl());
        assertTrue(second.isShowButton());

        // The sort order must not change and the other items must stay untouched
        assertEquals(2, second.getSortOrder());
        assertEquals("A", first.getTitle());
    }

    @Test
    void shouldDeleteTheReplacedImageFromCloudinary() {
        ItemBanner item = buildItem(1, "A", "artelier/banners/old", null);
        Banner banner = buildBanner(item);

        stubBanner(banner);
        when(itemBannerMapper.toResponse(item)).thenReturn(itemResponse(true, 1));

        service.updateItem(banner.getId(), item.getId(), updateRequest("artelier/banners/new", null));

        verify(cloudinaryService).delete("artelier/banners/old", CloudinaryService.RESOURCE_IMAGE);
        verifyNoMoreInteractions(cloudinaryService);
    }

    @Test
    void shouldDeleteTheReplacedModelFromCloudinaryAsRaw() {
        ItemBanner item = buildItem(1, "A", "artelier/banners/img", "artelier/banners/old-model");
        Banner banner = buildBanner(item);

        stubBanner(banner);
        when(itemBannerMapper.toResponse(item)).thenReturn(itemResponse(true, 1));

        service.updateItem(banner.getId(), item.getId(),
                updateRequest("artelier/banners/img", "artelier/banners/new-model"));

        verify(cloudinaryService).delete("artelier/banners/old-model", CloudinaryService.RESOURCE_RAW);
        verifyNoMoreInteractions(cloudinaryService); // the image did not change
    }

    @Test
    void shouldDeleteTheOldAssetWhenItIsRemovedFromTheItem() {
        // The update drops the 3D model (null id): the old file is no longer used
        ItemBanner item = buildItem(1, "A", "artelier/banners/img", "artelier/banners/model");
        Banner banner = buildBanner(item);

        stubBanner(banner);
        when(itemBannerMapper.toResponse(item)).thenReturn(itemResponse(true, 1));

        service.updateItem(banner.getId(), item.getId(), updateRequest("artelier/banners/img", null));

        verify(cloudinaryService).delete("artelier/banners/model", CloudinaryService.RESOURCE_RAW);
        verifyNoMoreInteractions(cloudinaryService);
    }

    @Test
    void shouldNotTouchCloudinaryWhenAssetsDoNotChange() {
        ItemBanner item = buildItem(1, "A", "artelier/banners/img", "artelier/banners/model");
        Banner banner = buildBanner(item);

        stubBanner(banner);
        when(itemBannerMapper.toResponse(item)).thenReturn(itemResponse(true, 1));

        service.updateItem(banner.getId(), item.getId(),
                updateRequest("artelier/banners/img", "artelier/banners/model"));

        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void shouldThrowIfItemNotFoundOnUpdateItem() {
        ItemBanner item = buildItem(1, "A");
        Banner banner = buildBanner(item);

        stubBanner(banner);

        assertThrows(ArtelierException.class,
                () -> service.updateItem(banner.getId(), UUID.randomUUID(), updateRequest()));

        assertEquals("A", item.getTitle());
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void shouldThrowIfBannerNotFoundOnUpdateItem() {
        when(bannerRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.updateItem(UUID.randomUUID(), UUID.randomUUID(), updateRequest()));

        verifyNoInteractions(cloudinaryService);
    }
}
