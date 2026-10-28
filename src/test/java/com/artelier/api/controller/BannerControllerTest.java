package com.artelier.api.controller;

import com.artelier.api.config.JacksonTestConfig;
import com.artelier.api.dto.request.BannerRequest;
import com.artelier.api.dto.request.ItemBannerRequest;
import com.artelier.api.dto.request.ItemBannerUpdateRequest;
import com.artelier.api.dto.response.BannerResponse;
import com.artelier.api.dto.response.ItemBannerResponse;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

    // =========================
    // PUBLIC - GET /banner
    // =========================

    @Test
    void shouldReturnActiveBanners() throws Exception {
        UUID bannerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(bannerService.getActiveBanners()).thenReturn(
                List.of(bannerResponse(bannerId, List.of(itemResponse(itemId, 1, "Summer"))))
        );

        mockMvc.perform(get("/banner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(bannerId.toString()))
                .andExpect(jsonPath("$[0].items.length()").value(1))
                .andExpect(jsonPath("$[0].items[0].id").value(itemId.toString()))
                .andExpect(jsonPath("$[0].items[0].title").value("Summer"))
                .andExpect(jsonPath("$[0].items[0].sortOrder").value(1));
    }

    @Test
    void shouldReturnEmptyListWhenNoActiveBanners() throws Exception {
        when(bannerService.getActiveBanners()).thenReturn(List.of());

        mockMvc.perform(get("/banner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
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
                .andExpect(jsonPath("$.id").value(bannerId.toString()))
                .andExpect(jsonPath("$.items[0].title").value("Item 1"));

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

        // Assumes the global exception handler maps ArtelierException.notFound to 404
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
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.sortOrder").value(3));

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
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.sortOrder").value(2));

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
    // ADMIN - UPDATE ITEM
    // =========================

    @Test
    void shouldUpdateItem() throws Exception {
        UUID bannerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(bannerService.updateItem(eq(bannerId), eq(1), any(ItemBannerUpdateRequest.class)))
                .thenReturn(itemResponse(itemId, 1, "Updated title"));

        mockMvc.perform(put("/admin/banners/" + bannerId + "/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateItemJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.title").value("Updated title"));

        ArgumentCaptor<ItemBannerUpdateRequest> captor =
                ArgumentCaptor.forClass(ItemBannerUpdateRequest.class);
        verify(bannerService).updateItem(eq(bannerId), eq(1), captor.capture());
        assertEquals("Shop now", captor.getValue().buttonLabel());
        assertTrue(captor.getValue().showButton());
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

        mockMvc.perform(put("/admin/banners/" + UUID.randomUUID() + "/items/1")
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

        mockMvc.perform(put("/admin/banners/" + UUID.randomUUID() + "/items/1")
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
    // ADMIN - DELETE ITEM
    // =========================

    @Test
    void shouldDeleteItem() throws Exception {
        UUID bannerId = UUID.randomUUID();

        mockMvc.perform(delete("/admin/banners/" + bannerId + "/items/2"))
                .andExpect(status().isNoContent());

        verify(bannerService).deleteItem(bannerId, 2);
    }

    @Test
    void shouldReturnBadRequestWhenServiceRejectsDeleteItemOrder() throws Exception {
        UUID bannerId = UUID.randomUUID();
        doThrow(ArtelierException.badRequest("Order must be between 1 and 3"))
                .when(bannerService).deleteItem(bannerId, 9);

        // Assumes the global exception handler maps ArtelierException.badRequest to 400
        mockMvc.perform(delete("/admin/banners/" + bannerId + "/items/9"))
                .andExpect(status().isBadRequest());
    }

    // =========================
    // ADMIN - REORDER ITEMS
    // =========================

    @Test
    void shouldReorderItems() throws Exception {
        UUID bannerId = UUID.randomUUID();

        mockMvc.perform(patch("/admin/banners/" + bannerId + "/items/reorder")
                        .param("oldOrder", "1")
                        .param("newOrder", "3"))
                .andExpect(status().isNoContent());

        verify(bannerService).reorderItem(bannerId, 1, 3);
    }

    @Test
    void shouldRejectReorderWhenParamIsMissing() throws Exception {
        mockMvc.perform(patch("/admin/banners/" + UUID.randomUUID() + "/items/reorder")
                        .param("oldOrder", "1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }

    @Test
    void shouldRejectReorderWhenParamIsNotANumber() throws Exception {
        mockMvc.perform(patch("/admin/banners/" + UUID.randomUUID() + "/items/reorder")
                        .param("oldOrder", "first")
                        .param("newOrder", "3"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bannerService);
    }
}
