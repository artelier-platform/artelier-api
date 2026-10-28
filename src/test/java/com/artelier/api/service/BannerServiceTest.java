package com.artelier.api.service;

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
import com.artelier.api.service.impl.BannerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class BannerServiceTest {

    @Mock private BannerRepository bannerRepository;
    @Mock private BannerMapper bannerMapper;
    @Mock private ItemBannerMapper itemBannerMapper;

    @InjectMocks
    private BannerServiceImpl service;

    // =========================
    // HELPERS
    // =========================

    private ItemBanner buildItem(int order, String title) {
        return ItemBanner.builder()
                .id(UUID.randomUUID())
                .sortOrder(order)
                .title(title)
                .backgroundImageUrl("https://res.cloudinary.com/artelier/image/upload/" + title + ".jpg")
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
        return new ItemBannerUpdateRequest(
                "New title", true,
                "https://res.cloudinary.com/artelier/image/upload/new.jpg",
                "artelier/banners/new",
                "https://res.cloudinary.com/artelier/raw/upload/model.glb",
                "artelier/banners/model",
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

        when(bannerRepository.findByIsActiveTrue()).thenReturn(List.of(banner));
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

        when(bannerRepository.findByIsActiveTrue()).thenReturn(List.of(banner));
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
        when(bannerRepository.findByIsActiveTrue()).thenReturn(List.of());

        List<BannerResponse> result = service.getActiveBanners();

        assertTrue(result.isEmpty());
        verify(bannerMapper, never()).toResponse(any());
    }

    // =========================
    // updateBannerStatus
    // =========================

    @Test
    void shouldUpdateBannerStatus() {
        Banner banner = buildBanner();
        assertTrue(banner.isActive());

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));

        service.updateBannerStatus(banner.getId(), false);

        assertFalse(banner.isActive());
        verify(bannerRepository).save(banner);
    }

    @Test
    void shouldThrowIfBannerNotFoundOnUpdateStatus() {
        when(bannerRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.updateBannerStatus(UUID.randomUUID(), true));

        verify(bannerRepository, never()).save(any());
    }

    // =========================
    // updateItemStatus
    // =========================

    @Test
    void shouldUpdateItemStatus() {
        ItemBanner item = buildItem(1, "A");
        Banner banner = buildBanner(item);

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));

        service.updateItemStatus(banner.getId(), item.getId(), false);

        assertFalse(item.isActive());
        verify(bannerRepository).save(banner);
    }

    @Test
    void shouldThrowIfItemNotFoundOnUpdateItemStatus() {
        Banner banner = buildBanner(buildItem(1, "A"));

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));

        assertThrows(ArtelierException.class,
                () -> service.updateItemStatus(banner.getId(), UUID.randomUUID(), false));

        verify(bannerRepository, never()).save(any());
    }

    @Test
    void shouldThrowIfBannerNotFoundOnUpdateItemStatus() {
        when(bannerRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.updateItemStatus(UUID.randomUUID(), UUID.randomUUID(), true));

        verify(bannerRepository, never()).save(any());
    }

    // =========================
    // deleteBanner
    // =========================

    @Test
    void shouldDeleteBanner() {
        Banner banner = buildBanner();

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));

        service.deleteBanner(banner.getId());

        verify(bannerRepository).delete(banner);
    }

    @Test
    void shouldThrowIfBannerNotFoundOnDelete() {
        when(bannerRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.deleteBanner(UUID.randomUUID()));

        verify(bannerRepository, never()).delete(any());
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

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));
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

        verify(bannerRepository).save(banner);
    }

    @Test
    void shouldAddFirstItemWithOrderOne() {
        Banner banner = buildBanner();

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));
        when(itemBannerMapper.toResponse(any(ItemBanner.class))).thenReturn(itemResponse(true, 1));

        service.addItem(banner.getId(), itemRequest(1, "A"));

        assertEquals(List.of(1), orders(banner));
    }

    @Test
    void shouldThrowIfBannerNotFoundOnAddItem() {
        when(bannerRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.addItem(UUID.randomUUID(), itemRequest(1, "A")));

        verify(bannerRepository, never()).save(any());
    }

    // =========================
    // addItem (insert at order)
    // =========================

    @Test
    void shouldInsertItemAtTheBeginningAndShiftTheRest() {
        Banner banner = buildBanner(buildItem(1, "A"), buildItem(2, "B"));
        ItemBannerResponse response = itemResponse(true, 1);

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));
        when(itemBannerMapper.toResponse(any(ItemBanner.class))).thenReturn(response);

        ItemBannerResponse result = service.addItem(banner.getId(), itemRequest(1, "NEW"), 1);

        assertSame(response, result);
        assertEquals(List.of("NEW", "A", "B"), titles(banner));
        assertEquals(List.of(1, 2, 3), orders(banner));
        banner.getItems().forEach(item -> assertSame(banner, item.getBanner()));
        verify(bannerRepository).save(banner);
    }

    @Test
    void shouldInsertItemInTheMiddle() {
        Banner banner = buildBanner(buildItem(1, "A"), buildItem(2, "B"), buildItem(3, "C"));

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));
        when(itemBannerMapper.toResponse(any(ItemBanner.class))).thenReturn(itemResponse(true, 2));

        service.addItem(banner.getId(), itemRequest(1, "NEW"), 2);

        assertEquals(List.of("A", "NEW", "B", "C"), titles(banner));
        assertEquals(List.of(1, 2, 3, 4), orders(banner));
    }

    @Test
    void shouldInsertItemAtTheEndWhenOrderIsSizePlusOne() {
        Banner banner = buildBanner(buildItem(1, "A"), buildItem(2, "B"));

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));
        when(itemBannerMapper.toResponse(any(ItemBanner.class))).thenReturn(itemResponse(true, 3));

        service.addItem(banner.getId(), itemRequest(1, "NEW"), 3);

        assertEquals(List.of("A", "B", "NEW"), titles(banner));
        assertEquals(List.of(1, 2, 3), orders(banner));
    }

    @Test
    void shouldThrowIfInsertOrderIsLessThanOne() {
        Banner banner = buildBanner(buildItem(1, "A"));

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));

        assertThrows(ArtelierException.class,
                () -> service.addItem(banner.getId(), itemRequest(1, "NEW"), 0));

        verify(bannerRepository, never()).save(any());
    }

    @Test
    void shouldThrowIfInsertOrderIsGreaterThanSizePlusOne() {
        Banner banner = buildBanner(buildItem(1, "A"));

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));

        assertThrows(ArtelierException.class,
                () -> service.addItem(banner.getId(), itemRequest(1, "NEW"), 3));

        assertEquals(1, banner.getItems().size());
        verify(bannerRepository, never()).save(any());
    }

    @Test
    void shouldThrowIfBannerNotFoundOnInsertItem() {
        when(bannerRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.addItem(UUID.randomUUID(), itemRequest(1, "A"), 1));

        verify(bannerRepository, never()).save(any());
    }

    // =========================
    // deleteItem
    // =========================

    @Test
    void shouldDeleteItemAndNormalizeOrder() {
        ItemBanner a = buildItem(1, "A");
        ItemBanner b = buildItem(2, "B");
        ItemBanner c = buildItem(3, "C");
        Banner banner = buildBanner(a, b, c);

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));

        service.deleteItem(banner.getId(), 2);

        assertEquals(List.of("A", "C"), titles(banner));
        assertEquals(List.of(1, 2), orders(banner));
        assertNull(b.getBanner());
        verify(bannerRepository).save(banner);
    }

    @Test
    void shouldDeleteTheOnlyItem() {
        Banner banner = buildBanner(buildItem(1, "A"));

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));

        service.deleteItem(banner.getId(), 1);

        assertTrue(banner.getItems().isEmpty());
        verify(bannerRepository).save(banner);
    }

    @Test
    void shouldThrowIfDeleteOrderIsOutOfRange() {
        Banner banner = buildBanner(buildItem(1, "A"), buildItem(2, "B"));

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));

        assertThrows(ArtelierException.class, () -> service.deleteItem(banner.getId(), 0));
        assertThrows(ArtelierException.class, () -> service.deleteItem(banner.getId(), 3));

        assertEquals(2, banner.getItems().size());
        verify(bannerRepository, never()).save(any());
    }

    @Test
    void shouldThrowIfBannerNotFoundOnDeleteItem() {
        when(bannerRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.deleteItem(UUID.randomUUID(), 1));

        verify(bannerRepository, never()).save(any());
    }

    // =========================
    // reorderItem
    // =========================

    @Test
    void shouldMoveItemForward() {
        Banner banner = buildBanner(buildItem(1, "A"), buildItem(2, "B"), buildItem(3, "C"));

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));

        service.reorderItem(banner.getId(), 1, 3);

        assertEquals(List.of("B", "C", "A"), titles(banner));
        assertEquals(List.of(1, 2, 3), orders(banner));
        verify(bannerRepository).save(banner);
    }

    @Test
    void shouldMoveItemBackward() {
        Banner banner = buildBanner(buildItem(1, "A"), buildItem(2, "B"), buildItem(3, "C"));

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));

        service.reorderItem(banner.getId(), 3, 1);

        assertEquals(List.of("C", "A", "B"), titles(banner));
        assertEquals(List.of(1, 2, 3), orders(banner));
        verify(bannerRepository).save(banner);
    }

    @Test
    void shouldDoNothingWhenOldAndNewOrderAreEqual() {
        Banner banner = buildBanner(buildItem(1, "A"), buildItem(2, "B"));

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));

        service.reorderItem(banner.getId(), 2, 2);

        assertEquals(List.of("A", "B"), titles(banner));
        verify(bannerRepository, never()).save(any());
    }

    @Test
    void shouldThrowIfReorderOldOrderIsInvalid() {
        Banner banner = buildBanner(buildItem(1, "A"), buildItem(2, "B"));

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));

        assertThrows(ArtelierException.class, () -> service.reorderItem(banner.getId(), 0, 1));
        assertThrows(ArtelierException.class, () -> service.reorderItem(banner.getId(), 3, 1));

        verify(bannerRepository, never()).save(any());
    }

    @Test
    void shouldThrowIfReorderNewOrderIsInvalid() {
        Banner banner = buildBanner(buildItem(1, "A"), buildItem(2, "B"));

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));

        assertThrows(ArtelierException.class, () -> service.reorderItem(banner.getId(), 1, 0));
        assertThrows(ArtelierException.class, () -> service.reorderItem(banner.getId(), 1, 3));

        assertEquals(List.of("A", "B"), titles(banner));
        verify(bannerRepository, never()).save(any());
    }

    @Test
    void shouldThrowIfBannerNotFoundOnReorder() {
        when(bannerRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.reorderItem(UUID.randomUUID(), 1, 2));

        verify(bannerRepository, never()).save(any());
    }

    // =========================
    // updateItem
    // =========================

    @Test
    void shouldUpdateItemFields() {
        ItemBanner first = buildItem(1, "A");
        ItemBanner second = buildItem(2, "B");
        Banner banner = buildBanner(first, second);
        ItemBannerResponse response = itemResponse(true, 2);

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));
        when(itemBannerMapper.toResponse(second)).thenReturn(response);

        ItemBannerResponse result = service.updateItem(banner.getId(), 2, updateRequest());

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

        verify(bannerRepository).save(banner);
    }

    @Test
    void shouldThrowIfUpdateItemOrderIsOutOfRange() {
        Banner banner = buildBanner(buildItem(1, "A"));

        when(bannerRepository.findById(banner.getId())).thenReturn(Optional.of(banner));

        assertThrows(ArtelierException.class,
                () -> service.updateItem(banner.getId(), 0, updateRequest()));
        assertThrows(ArtelierException.class,
                () -> service.updateItem(banner.getId(), 2, updateRequest()));

        assertEquals("A", banner.getItems().getFirst().getTitle());
        verify(bannerRepository, never()).save(any());
    }

    @Test
    void shouldThrowIfBannerNotFoundOnUpdateItem() {
        when(bannerRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.updateItem(UUID.randomUUID(), 1, updateRequest()));

        verify(bannerRepository, never()).save(any());
    }
}
