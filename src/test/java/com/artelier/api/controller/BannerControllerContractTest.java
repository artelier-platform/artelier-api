package com.artelier.api.controller;

import com.artelier.api.config.JacksonTestConfig;
import com.artelier.api.dto.response.BannerResponse;
import com.artelier.api.dto.response.ItemBannerResponse;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.BannerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract of the public {@code GET /banners} endpoint: full payload mapping, ordering,
 * delegation to the service and error translation.
 * Access rules live in {@link BannerControllerSecurityTest}.
 */
@WebMvcTest(BannerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonTestConfig.class)
@DisplayName("BannerController – GET /banners contract")
class BannerControllerContractTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-27T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BannerService bannerService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    private static ItemBannerResponse simpleItem(int sortOrder, String title) {
        return new ItemBannerResponse(
                UUID.randomUUID(), true, sortOrder, title, true,
                "https://res.cloudinary.com/artelier/image/upload/bg.jpg", "artelier/banners/bg",
                null, null,
                null, false,
                null, null, false);
    }

    private static BannerResponse banner(UUID id, ItemBannerResponse... items) {
        return new BannerResponse(id, true, CREATED_AT, List.of(items));
    }

    @Test
    @DisplayName("200 – maps every banner and item field to the JSON payload")
    void mapsAllFields() throws Exception {
        UUID bannerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        ItemBannerResponse item = new ItemBannerResponse(
                itemId, true, 2, "Summer", true,
                "https://res.cloudinary.com/artelier/image/upload/bg.jpg", "artelier/banners/bg",
                "https://res.cloudinary.com/artelier/raw/upload/model.glb", "artelier/banners/model",
                "Pieces crafted by local artisans", true,
                "Shop now", "https://artelier-web.vercel.app/products", true);

        when(bannerService.getActiveBanners()).thenReturn(List.of(banner(bannerId, item)));

        mockMvc.perform(get("/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Banners retrieved"))
                .andExpect(jsonPath("$.data[0].id").value(bannerId.toString()))
                .andExpect(jsonPath("$.data[0].isActive").value(true))
                .andExpect(jsonPath("$.data[0].createdAt").value("2026-10-27T10:00:00Z"))
                .andExpect(jsonPath("$.data[0].items[0].id").value(itemId.toString()))
                .andExpect(jsonPath("$.data[0].items[0].isActive").value(true))
                .andExpect(jsonPath("$.data[0].items[0].sortOrder").value(2))
                .andExpect(jsonPath("$.data[0].items[0].title").value("Summer"))
                .andExpect(jsonPath("$.data[0].items[0].showTitle").value(true))
                .andExpect(jsonPath("$.data[0].items[0].backgroundImageUrl")
                        .value("https://res.cloudinary.com/artelier/image/upload/bg.jpg"))
                .andExpect(jsonPath("$.data[0].items[0].backgroundImageCloudinaryId").value("artelier/banners/bg"))
                .andExpect(jsonPath("$.data[0].items[0].model3dUrl")
                        .value("https://res.cloudinary.com/artelier/raw/upload/model.glb"))
                .andExpect(jsonPath("$.data[0].items[0].model3dPublicId").value("artelier/banners/model"))
                .andExpect(jsonPath("$.data[0].items[0].description").value("Pieces crafted by local artisans"))
                .andExpect(jsonPath("$.data[0].items[0].showDescription").value(true))
                .andExpect(jsonPath("$.data[0].items[0].buttonLabel").value("Shop now"))
                .andExpect(jsonPath("$.data[0].items[0].buttonUrl")
                        .value("https://artelier-web.vercel.app/products"))
                .andExpect(jsonPath("$.data[0].items[0].showButton").value(true));
    }

    @Test
    @DisplayName("200 – keeps the order of banners and items returned by the service")
    void preservesOrdering() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();

        when(bannerService.getActiveBanners()).thenReturn(List.of(
                banner(first, simpleItem(1, "A1"), simpleItem(2, "A2"), simpleItem(3, "A3")),
                banner(second, simpleItem(1, "B1")),
                banner(third, simpleItem(1, "C1"), simpleItem(2, "C2"))));

        mockMvc.perform(get("/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].id").value(first.toString()))
                .andExpect(jsonPath("$.data[1].id").value(second.toString()))
                .andExpect(jsonPath("$.data[2].id").value(third.toString()))
                .andExpect(jsonPath("$.data[0].items.length()").value(3))
                .andExpect(jsonPath("$.data[0].items[0].title").value("A1"))
                .andExpect(jsonPath("$.data[0].items[1].title").value("A2"))
                .andExpect(jsonPath("$.data[0].items[2].title").value("A3"))
                .andExpect(jsonPath("$.data[2].items[1].title").value("C2"));
    }

    @Test
    @DisplayName("200 – empty result is an empty list inside the standard envelope")
    void emptyListEnvelope() throws Exception {
        when(bannerService.getActiveBanners()).thenReturn(List.of());

        mockMvc.perform(get("/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Banners retrieved"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("200 – responds with a JSON body")
    void respondsWithJson() throws Exception {
        when(bannerService.getActiveBanners()).thenReturn(List.of());

        mockMvc.perform(get("/banners"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Delegates exactly once to the service and ignores unknown query parameters")
    void delegatesOnce() throws Exception {
        when(bannerService.getActiveBanners()).thenReturn(List.of());

        mockMvc.perform(get("/banners").param("unknown", "value"))
                .andExpect(status().isOk());

        verify(bannerService, times(1)).getActiveBanners();
        verifyNoMoreInteractions(bannerService);
    }

    @Test
    @DisplayName("500 – unexpected failure is mapped to the generic error body")
    void unexpectedFailure() throws Exception {
        when(bannerService.getActiveBanners()).thenThrow(new IllegalStateException("db down"));

        mockMvc.perform(get("/banners"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Something went wrong"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("ArtelierException keeps its own status and message")
    void artelierExceptionIsTranslated() throws Exception {
        when(bannerService.getActiveBanners())
                .thenThrow(new ArtelierException("Banners unavailable", HttpStatus.SERVICE_UNAVAILABLE));

        mockMvc.perform(get("/banners"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Banners unavailable"));
    }
}
