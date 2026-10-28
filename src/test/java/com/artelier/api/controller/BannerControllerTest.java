package com.artelier.api.controller;

import com.artelier.api.config.JacksonTestConfig;
import com.artelier.api.dto.request.BannerRequest;
import com.artelier.api.dto.request.ItemBannerRequest;
import com.artelier.api.dto.request.ItemBannerUpdateRequest;
import com.artelier.api.dto.response.BannerResponse;
import com.artelier.api.dto.response.ItemBannerResponse;
import com.artelier.api.dto.response.UploadedAssetResponse;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.BannerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BannerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonTestConfig.class)
class BannerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BannerService bannerService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    // =========================
    // HELPERS
    // =========================

    /** Valid item payload: has a background image, title shown, nothing else enabled. */
    private String itemJson(int sortOrder) {
        return """
                {
                  "sortOrder": %d,
                  "title": "Item %d",
                  "showTitle": true,
                  "backgroundImageUrl": "https://res.cloudinary.com/artelier/image/upload/bg.jpg",
                  "backgroundImageCloudinaryId": "artelier/banners/bg",
                  "description": "Some description",
                  "showDescription": false,
                  "showButton": false
                }
                """.formatted(sortOrder, sortOrder);
    }

    /** Valid payload for the "update item" endpoint (no sortOrder). */
    private String updateItemJson() {
        return """
                {
                  "title": "Updated title",
                  "showTitle": true,
                  "backgroundImageUrl": "https://res.cloudinary.com/artelier/image/upload/new.jpg",
                  "backgroundImageCloudinaryId": "artelier/banners/new",
                  "description": "Updated description",
                  "showDescription": true,
                  "buttonLabel": "Shop now",
                  "buttonUrl": "https://artelier.com/shop",
                  "showButton": true
                }
                """;
    }

    /** Invalid item payload: no background source at all. */
    private String itemWithoutBackgroundJson() {
        return """
                {
                  "sortOrder": 1,
                  "title": "No background",
                  "showTitle": true,
                  "showDescription": false,
                  "showButton": false
                }
                """;
    }

    private String bannerJson(String itemsJson) {
        return """
                {
                  "isActive": true,
                  "items": [%s]
                }
                """.formatted(itemsJson);
    }

    private ItemBannerResponse itemResponse(UUID id, int order, String title) {
        return new ItemBannerResponse(
                id, true, order, title, true,
                "https://res.cloudinary.com/artelier/image/upload/bg.jpg",
                "artelier/banners/bg",
                null, null,
                "Some description", false,
                null, null, false
        );
    }

    private BannerResponse bannerResponse(UUID id, List<ItemBannerResponse> items) {
        return new BannerResponse(id, true, Instant.parse("2026-10-27T10:00:00Z"), items);
    }

    /** First bytes of a real PNG, enough for a multipart upload in a controller test. */
    private MockMultipartFile pngFile() {
        byte[] png = {
                (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D
        };
        return new MockMultipartFile("file", "banner.png", "image/png", png);
    }

    // =========================
    // PUBLIC - GET /banners
    // =========================

    @Test
    void shouldReturnActiveBanners() throws Exception {
        UUID bannerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(bannerService.getActiveBanners()).thenReturn(
                List.of(bannerResponse(bannerId, List.of(itemResponse(itemId, 1, "Summer"))))
        );

        mockMvc.perform(get("/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(bannerId.toString()))
                .andExpect(jsonPath("$.data[0].items.length()").value(1))
                .andExpect(jsonPath("$.data[0].items[0].id").value(itemId.toString()))
                .andExpect(jsonPath("$.data[0].items[0].title").value("Summer"))
                .andExpect(jsonPath("$.data[0].items[0].sortOrder").value(1));
    }

    @Test
    void shouldReturnEmptyListWhenNoActiveBanners() throws Exception {
        when(bannerService.getActiveBanners()).thenReturn(List.of());

        mockMvc.perform(get("/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // =========================
    // ADMIN - GET /admin/banners
    // =========================

    @Test
    void shouldReturnAllBannersForAdmin() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        when(bannerService.getAllBanners()).thenReturn(List.of(
                bannerResponse(first, List.of(itemResponse(UUID.randomUUID(), 1, "A"))),
                bannerResponse(second, List.of())
        ));

        mockMvc.perform(get("/admin/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(first.toString()))
                .andExpect(jsonPath("$.data[1].id").value(second.toString()))
                .andExpect(jsonPath("$.data[1].items.length()").value(0));

        verify(bannerService).getAllBanners();
    }

    // =========================
    // ADMIN - UPLOAD IMAGE
    // =========================

    @Test
    void shouldUploadImage() throws Exception {
        when(bannerService.uploadImage(any()))
                .thenReturn(new UploadedAssetResponse(
                        "https://res.cloudinary.com/artelier/image/upload/banners/new.png",
                        "artelier/banners/new"
                ));

        mockMvc.perform(multipart("/admin/banners/uploads").file(pngFile()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url")
                        .value("https://res.cloudinary.com/artelier/image/upload/banners/new.png"))
                .andExpect(jsonPath("$.data.publicId").value("artelier/banners/new"));

        verify(bannerService).uploadImage(any());
    }

    @Test
    void shouldRejectUploadWhenFilePartIsMissing() throws Exception {
        mockMvc.perform(multipart("/admin/banners/uploads"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    @Test
    void shouldReturnBadRequestWhenServiceRejectsTheImage() throws Exception {
        when(bannerService.uploadImage(any()))
                .thenThrow(ArtelierException.badRequest("Invalid image"));

        mockMvc.perform(multipart("/admin/banners/uploads").file(pngFile()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnPayloadTooLargeWhenFileExceedsTheLimit() throws Exception {
        when(bannerService.uploadImage(any()))
                .thenThrow(new MaxUploadSizeExceededException(5L * 1024 * 1024));

        mockMvc.perform(multipart("/admin/banners/uploads").file(pngFile()))
                .andExpect(status().isPayloadTooLarge());
    }

    // =========================
    // ADMIN - CREATE BANNER
    // =========================

    @Test
    void shouldCreateBanner() throws Exception {
        UUID bannerId = UUID.randomUUID();
        when(bannerService.createBanner(any(BannerRequest.class)))
                .thenReturn(bannerResponse(bannerId, List.of(itemResponse(UUID.randomUUID(), 1, "Item 1"))));

        mockMvc.perform(post("/admin/banners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bannerJson(itemJson(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(bannerId.toString()))
                .andExpect(jsonPath("$.data.items[0].title").value("Item 1"));

        ArgumentCaptor<BannerRequest> captor = ArgumentCaptor.forClass(BannerRequest.class);
        verify(bannerService).createBanner(captor.capture());
        assertEquals(1, captor.getValue().items().size());
        assertEquals("Item 1", captor.getValue().items().getFirst().title());
    }

    @Test
    void shouldRejectCreateBannerWhenItemsAreMissing() throws Exception {
        mockMvc.perform(post("/admin/banners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\": true}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    @Test
    void shouldRejectCreateBannerWhenItemsAreEmpty() throws Exception {
        mockMvc.perform(post("/admin/banners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bannerJson("")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    @Test
    void shouldRejectCreateBannerWhenMoreThanTwentyItems() throws Exception {
        String items = IntStream.rangeClosed(1, 21)
                .mapToObj(this::itemJson)
                .collect(Collectors.joining(","));

        mockMvc.perform(post("/admin/banners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bannerJson(items)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    @Test
    void shouldRejectCreateBannerWhenAnItemIsInvalid() throws Exception {
        mockMvc.perform(post("/admin/banners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bannerJson(itemWithoutBackgroundJson())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    // =========================
    // ADMIN - BANNER STATUS / DELETE
    // =========================

    @Test
    void shouldUpdateBannerStatus() throws Exception {
        UUID bannerId = UUID.randomUUID();

        mockMvc.perform(patch("/admin/banners/" + bannerId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\": false}"))
                .andExpect(status().isNoContent());

        verify(bannerService).updateBannerStatus(bannerId, false);
    }

    @Test
    void shouldRejectBannerStatusWhenIsActiveIsMissing() throws Exception {
        mockMvc.perform(patch("/admin/banners/" + UUID.randomUUID() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    @Test
    void shouldRejectBannerStatusWhenIdIsNotAUuid() throws Exception {
        mockMvc.perform(patch("/admin/banners/not-a-uuid/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\": true}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    @Test
    void shouldDeleteBanner() throws Exception {
        UUID bannerId = UUID.randomUUID();

        mockMvc.perform(delete("/admin/banners/" + bannerId))
                .andExpect(status().isNoContent());

        verify(bannerService).deleteBanner(bannerId);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingBanner() throws Exception {
        UUID bannerId = UUID.randomUUID();
        doThrow(ArtelierException.notFound("Banner not found"))
                .when(bannerService).deleteBanner(bannerId);

        mockMvc.perform(delete("/admin/banners/" + bannerId))
                .andExpect(status().isNotFound());
    }

    // =========================
    // ADMIN - ADD ITEM
    // =========================

    @Test
    void shouldAddItemAtTheEnd() throws Exception {
        UUID bannerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(bannerService.addItem(eq(bannerId), any(ItemBannerRequest.class)))
                .thenReturn(itemResponse(itemId, 3, "Item 3"));

        mockMvc.perform(post("/admin/banners/" + bannerId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson(3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(itemId.toString()))
                .andExpect(jsonPath("$.data.sortOrder").value(3));

        verify(bannerService).addItem(eq(bannerId), any(ItemBannerRequest.class));
    }

    @Test
    void shouldRejectAddItemWhenBackgroundSourceIsMissing() throws Exception {
        mockMvc.perform(post("/admin/banners/" + UUID.randomUUID() + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemWithoutBackgroundJson()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    @Test
    void shouldRejectAddItemWhenSortOrderIsLessThanOne() throws Exception {
        mockMvc.perform(post("/admin/banners/" + UUID.randomUUID() + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson(0)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    @Test
    void shouldRejectAddItemWhenButtonIsEnabledWithoutLabelAndUrl() throws Exception {
        String json = """
                {
                  "sortOrder": 1,
                  "title": "Item",
                  "showTitle": true,
                  "backgroundImageUrl": "https://res.cloudinary.com/artelier/image/upload/bg.jpg",
                  "showDescription": false,
                  "showButton": true
                }
                """;

        mockMvc.perform(post("/admin/banners/" + UUID.randomUUID() + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    @Test
    void shouldInsertItemAtGivenOrder() throws Exception {
        UUID bannerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(bannerService.addItem(eq(bannerId), any(ItemBannerRequest.class), eq(2)))
                .thenReturn(itemResponse(itemId, 2, "Inserted"));

        mockMvc.perform(post("/admin/banners/" + bannerId + "/items/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson(1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(itemId.toString()))
                .andExpect(jsonPath("$.data.sortOrder").value(2));

        verify(bannerService).addItem(eq(bannerId), any(ItemBannerRequest.class), eq(2));
    }

    @Test
    void shouldRejectInsertItemWhenOrderIsNotANumber() throws Exception {
        mockMvc.perform(post("/admin/banners/" + UUID.randomUUID() + "/items/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson(1)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    // =========================
    // ADMIN - UPDATE ITEM (by itemId)
    // =========================

    @Test
    void shouldUpdateItem() throws Exception {
        UUID bannerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(bannerService.updateItem(eq(bannerId), eq(itemId), any(ItemBannerUpdateRequest.class)))
                .thenReturn(itemResponse(itemId, 1, "Updated title"));

        mockMvc.perform(put("/admin/banners/" + bannerId + "/items/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateItemJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(itemId.toString()))
                .andExpect(jsonPath("$.data.title").value("Updated title"));

        ArgumentCaptor<ItemBannerUpdateRequest> captor =
                ArgumentCaptor.forClass(ItemBannerUpdateRequest.class);
        verify(bannerService).updateItem(eq(bannerId), eq(itemId), captor.capture());
        assertEquals("Shop now", captor.getValue().buttonLabel());
        assertTrue(captor.getValue().showButton());
    }

    @Test
    void shouldRejectUpdateItemWhenItemIdIsNotAUuid() throws Exception {
        mockMvc.perform(put("/admin/banners/" + UUID.randomUUID() + "/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateItemJson()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingItem() throws Exception {
        UUID bannerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(bannerService.updateItem(eq(bannerId), eq(itemId), any(ItemBannerUpdateRequest.class)))
                .thenThrow(ArtelierException.notFound("Banner item not found"));

        mockMvc.perform(put("/admin/banners/" + bannerId + "/items/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateItemJson()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectUpdateItemWhenTitleIsRequiredButMissing() throws Exception {
        String json = """
                {
                  "showTitle": true,
                  "backgroundImageUrl": "https://res.cloudinary.com/artelier/image/upload/bg.jpg",
                  "showDescription": false,
                  "showButton": false
                }
                """;

        mockMvc.perform(put("/admin/banners/" + UUID.randomUUID() + "/items/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    @Test
    void shouldRejectUpdateItemWhenTitleIsTooLong() throws Exception {
        String json = """
                {
                  "title": "%s",
                  "showTitle": true,
                  "backgroundImageUrl": "https://res.cloudinary.com/artelier/image/upload/bg.jpg",
                  "showDescription": false,
                  "showButton": false
                }
                """.formatted("a".repeat(121));

        mockMvc.perform(put("/admin/banners/" + UUID.randomUUID() + "/items/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    // =========================
    // ADMIN - ITEM STATUS
    // =========================

    @Test
    void shouldUpdateItemStatus() throws Exception {
        UUID bannerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(patch("/admin/banners/" + bannerId + "/items/" + itemId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\": true}"))
                .andExpect(status().isNoContent());

        verify(bannerService).updateItemStatus(bannerId, itemId, true);
    }

    @Test
    void shouldRejectItemStatusWhenIsActiveIsMissing() throws Exception {
        mockMvc.perform(patch("/admin/banners/" + UUID.randomUUID() + "/items/" + UUID.randomUUID() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    // =========================
    // ADMIN - MOVE ITEM (position)
    // =========================

    @Test
    void shouldMoveItem() throws Exception {
        UUID bannerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(patch("/admin/banners/" + bannerId + "/items/" + itemId + "/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newOrder\": 3}"))
                .andExpect(status().isNoContent());

        verify(bannerService).moveItem(bannerId, itemId, 3);
    }

    @Test
    void shouldRejectMoveItemWhenNewOrderIsMissing() throws Exception {
        mockMvc.perform(patch("/admin/banners/" + UUID.randomUUID() + "/items/" + UUID.randomUUID() + "/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    @Test
    void shouldRejectMoveItemWhenNewOrderIsLessThanOne() throws Exception {
        mockMvc.perform(patch("/admin/banners/" + UUID.randomUUID() + "/items/" + UUID.randomUUID() + "/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newOrder\": 0}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    @Test
    void shouldRejectMoveItemWhenNewOrderIsNotANumber() throws Exception {
        mockMvc.perform(patch("/admin/banners/" + UUID.randomUUID() + "/items/" + UUID.randomUUID() + "/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newOrder\": \"first\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    @Test
    void shouldRejectMoveItemWhenItemIdIsNotAUuid() throws Exception {
        mockMvc.perform(patch("/admin/banners/" + UUID.randomUUID() + "/items/2/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newOrder\": 1}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    @Test
    void shouldReturnBadRequestWhenServiceRejectsTheNewOrder() throws Exception {
        UUID bannerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        doThrow(ArtelierException.badRequest("Order must be between 1 and 3"))
                .when(bannerService).moveItem(bannerId, itemId, 9);

        mockMvc.perform(patch("/admin/banners/" + bannerId + "/items/" + itemId + "/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newOrder\": 9}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenMovingMissingItem() throws Exception {
        UUID bannerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        doThrow(ArtelierException.notFound("Banner item not found"))
                .when(bannerService).moveItem(bannerId, itemId, 1);

        mockMvc.perform(patch("/admin/banners/" + bannerId + "/items/" + itemId + "/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newOrder\": 1}"))
                .andExpect(status().isNotFound());
    }

    // =========================
    // ADMIN - DELETE ITEM (by itemId)
    // =========================

    @Test
    void shouldDeleteItem() throws Exception {
        UUID bannerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(delete("/admin/banners/" + bannerId + "/items/" + itemId))
                .andExpect(status().isNoContent());

        verify(bannerService).deleteItem(bannerId, itemId);
    }

    @Test
    void shouldRejectDeleteItemWhenItemIdIsNotAUuid() throws Exception {
        mockMvc.perform(delete("/admin/banners/" + UUID.randomUUID() + "/items/2"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingItem() throws Exception {
        UUID bannerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        doThrow(ArtelierException.notFound("Banner item not found"))
                .when(bannerService).deleteItem(bannerId, itemId);

        mockMvc.perform(delete("/admin/banners/" + bannerId + "/items/" + itemId))
                .andExpect(status().isNotFound());
    }
}
