package com.artelier.api.controller;

import com.artelier.api.config.SecurityConfig;
import com.artelier.api.dto.response.BannerResponse;
import com.artelier.api.dto.response.CategoryResponse;
import com.artelier.api.dto.response.ItemBannerResponse;
import com.artelier.api.dto.response.ProductResponse;
import com.artelier.api.dto.response.UploadedAssetResponse;
import com.artelier.api.enums.StockType;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.BannerService;
import com.artelier.api.service.CategoryService;
import com.artelier.api.service.ProductService;
import com.artelier.api.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminController")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // The controller injects 4 services: all of them must be mocked.
    @MockitoBean private UserService userService;
    @MockitoBean private BannerService bannerService;
    @MockitoBean private CategoryService categoryService;
    @MockitoBean private ProductService productService;

    // Security dependencies (JwtFilter / SecurityConfig).
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserRepository userRepository;

    private static final String ADMIN = "ADMIN";

    // =========================================================
    // Fixtures
    // =========================================================

    private static String itemJson(int sortOrder) {
        return """
                {
                  "sortOrder": %d,
                  "title": "Handmade Ceramics",
                  "showTitle": true,
                  "backgroundImageUrl": "https://res.cloudinary.com/demo/image/upload/banner.jpg",
                  "backgroundImageCloudinaryId": "artelier/banners/banner",
                  "model3dUrl": null,
                  "model3dPublicId": null,
                  "description": null,
                  "showDescription": false,
                  "buttonLabel": null,
                  "buttonUrl": null,
                  "showButton": false
                }
                """.formatted(sortOrder);
    }

    private static final String ITEM_UPDATE_JSON = """
            {
              "title": "Updated artwork",
              "showTitle": true,
              "backgroundImageUrl": "https://res.cloudinary.com/demo/image/upload/banner.jpg",
              "backgroundImageCloudinaryId": "artelier/banners/banner",
              "model3dUrl": null,
              "model3dPublicId": null,
              "description": null,
              "showDescription": false,
              "buttonLabel": null,
              "buttonUrl": null,
              "showButton": false
            }
            """;

    private static String productJson(UUID categoryId) {
        return """
                {
                  "categoryId": "%s",
                  "name": "Handmade Ceramic Mug",
                  "description": "Beautiful handmade mug",
                  "story": null,
                  "price": 49.99,
                  "stockType": "AVAILABLE",
                  "stockQuantity": 10,
                  "isCustomOrder": false,
                  "isActive": true
                }
                """.formatted(categoryId);
    }

    private static MockMultipartFile dataPart(String json) {
        return new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes());
    }

    private static MockMultipartFile imagePart() {
        return new MockMultipartFile("images", "mug.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});
    }

    private static ItemBannerResponse itemResponse(UUID id, String title) {
        return new ItemBannerResponse(id, true, 1, title, true,
                "https://res.cloudinary.com/demo/image/upload/banner.jpg", "artelier/banners/banner",
                null, null, null, false, null, null, false);
    }

    private static ProductResponse productResponse(UUID id, boolean active) {
        return new ProductResponse(id,
                new CategoryResponse(UUID.randomUUID(), "Ceramics", "ceramics", null),
                "Handmade Ceramic Mug", "handmade-ceramic-mug", null, null,
                new BigDecimal("49.99"), StockType.AVAILABLE, 10, false, active,
                Instant.parse("2026-01-15T10:30:00Z"), List.of());
    }

    // =========================================================
    // CROSS-CUTTING SECURITY (ALL ENDPOINTS)
    // =========================================================

    // SecurityConfig requires ROLE_ADMIN for /admin/** at the URL level, so the 401/403
    // occurs in the filter before resolving the body or multipart request; no content is needed.
    static Stream<Arguments> allEndpoints() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        return Stream.of(
                ep("PATCH /admin/users/{id}/ban", () -> patch("/admin/users/{id}/ban", a)),
                ep("PATCH /admin/users/{id}/unban", () -> patch("/admin/users/{id}/unban", a)),
                ep("GET /admin/users", () -> get("/admin/users")),
                ep("GET /admin/banners", () -> get("/admin/banners")),
                ep("POST /admin/banners/uploads", () -> post("/admin/banners/uploads")),
                ep("POST /admin/banners", () -> post("/admin/banners")),
                ep("PATCH /admin/banners/{id}/status", () -> patch("/admin/banners/{id}/status", a)),
                ep("DELETE /admin/banners/{id}", () -> delete("/admin/banners/{id}", a)),
                ep("POST /admin/banners/{id}/items", () -> post("/admin/banners/{id}/items", a)),
                ep("POST /admin/banners/{id}/items/{order}", () -> post("/admin/banners/{id}/items/{o}", a, 1)),
                ep("PUT /admin/banners/{id}/items/{itemId}", () -> put("/admin/banners/{id}/items/{i}", a, b)),
                ep("PATCH /admin/banners/{id}/items/{itemId}/status",
                        () -> patch("/admin/banners/{id}/items/{i}/status", a, b)),
                ep("PATCH /admin/banners/{id}/items/{itemId}/position",
                        () -> patch("/admin/banners/{id}/items/{i}/position", a, b)),
                ep("DELETE /admin/banners/{id}/items/{itemId}",
                        () -> delete("/admin/banners/{id}/items/{i}", a, b)),
                ep("POST /admin/categories", () -> post("/admin/categories")),
                ep("PATCH /admin/products/{id}/toggle", () -> patch("/admin/products/{id}/toggle", a)),
                ep("DELETE /admin/products/{id}", () -> delete("/admin/products/{id}", a)),
                ep("PUT /admin/products/{id}", () -> put("/admin/products/{id}", a)),
                ep("POST /admin/products", () -> post("/admin/products"))
        );
    }

    private static Arguments ep(String name, Supplier<MockHttpServletRequestBuilder> request) {
        return Arguments.of(name, request);
    }

    @ParameterizedTest(name = "401 unauthenticated – {0}")
    @MethodSource("allEndpoints")
    @DisplayName("401 – unauthenticated requests are rejected")
    void unauthenticated_isRejected(String name, Supplier<MockHttpServletRequestBuilder> request) throws Exception {
        mockMvc.perform(request.get())
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService, bannerService, categoryService, productService);
    }

    @ParameterizedTest(name = "403 BUYER role – {0}")
    @MethodSource("allEndpoints")
    @WithMockUser(roles = "BUYER")
    @DisplayName("403 – users without the ADMIN role are rejected")
    void nonAdmin_isForbidden(String name, Supplier<MockHttpServletRequestBuilder> request) throws Exception {
        mockMvc.perform(request.get())
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService, bannerService, categoryService, productService);
    }

    // =========================================================
    // USERS
    // =========================================================

    @Nested
    @DisplayName("PATCH /admin/users/{id}/ban")
    class BanUser {

        @Test
        @DisplayName("200 – admin bans a user successfully")
        @WithMockUser(roles = ADMIN)
        void banUser_success() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(userService).setBanned(id, true);

            mockMvc.perform(patch("/admin/users/{id}/ban", id).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User banned"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(userService).setBanned(id, true);
        }

        @Test
        @DisplayName("400 – malformed UUID")
        @WithMockUser(roles = ADMIN)
        void banUser_invalidUuid() throws Exception {
            mockMvc.perform(patch("/admin/users/{id}/ban", "not-a-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));

            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("PATCH /admin/users/{id}/unban")
    class UnbanUser {

        @Test
        @DisplayName("200 – admin unbans a user successfully")
        @WithMockUser(roles = ADMIN)
        void unbanUser_success() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(userService).setBanned(id, false);

            mockMvc.perform(patch("/admin/users/{id}/unban", id).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User unbanned"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(userService).setBanned(id, false);
        }
    }

    @Nested
    @DisplayName("GET /admin/users")
    class ListUsers {

        @Test
        @DisplayName("200 – uses page=0 and size=20 by default")
        @WithMockUser(roles = ADMIN)
        void listUsers_defaults() throws Exception {
            when(userService.listUsers(0, 20)).thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Users retrieved"))
                    .andExpect(jsonPath("$.data.content").isArray());

            verify(userService).listUsers(0, 20);
        }

        @Test
        @DisplayName("200 – respects the provided page and size")
        @WithMockUser(roles = ADMIN)
        void listUsers_customParams() throws Exception {
            when(userService.listUsers(2, 50)).thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/admin/users").param("page", "2").param("size", "50"))
                    .andExpect(status().isOk());

            verify(userService).listUsers(2, 50);
        }

        @Test
        @DisplayName("400 – page is not numeric")
        @WithMockUser(roles = ADMIN)
        void listUsers_invalidPageType() throws Exception {
            mockMvc.perform(get("/admin/users").param("page", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid value for parameter 'page'"));

            verifyNoInteractions(userService);
        }
    }

    // =========================================================
    // BANNERS
    // =========================================================

    @Nested
    @DisplayName("GET /admin/banners")
    class GetAllBanners {

        @Test
        @DisplayName("200 – returns the list of banners with their items")
        @WithMockUser(roles = ADMIN)
        void getAllBanners_success() throws Exception {
            UUID bannerId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            when(bannerService.getAllBanners()).thenReturn(List.of(
                    new BannerResponse(bannerId, true, Instant.parse("2026-01-15T10:30:00Z"),
                            List.of(itemResponse(itemId, "Featured artwork")))));

            mockMvc.perform(get("/admin/banners"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Banners retrieved"))
                    .andExpect(jsonPath("$.data[0].id").value(bannerId.toString()))
                    .andExpect(jsonPath("$.data[0].items[0].id").value(itemId.toString()))
                    .andExpect(jsonPath("$.data[0].items[0].title").value("Featured artwork"));
        }
    }

    @Nested
    @DisplayName("POST /admin/banners/uploads")
    class UploadBannerImage {

        @Test
        @DisplayName("201 – uploads an image and returns the URL and publicId")
        @WithMockUser(roles = ADMIN)
        void upload_success() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "banner.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});
            when(bannerService.uploadImage(any())).thenReturn(
                    new UploadedAssetResponse("https://res.cloudinary.com/example/banner.jpg", "artelier/banners/banner"));

            mockMvc.perform(multipart("/admin/banners/uploads").file(file))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Image uploaded"))
                    .andExpect(jsonPath("$.data.url").value("https://res.cloudinary.com/example/banner.jpg"))
                    .andExpect(jsonPath("$.data.publicId").value("artelier/banners/banner"));

            verify(bannerService).uploadImage(any());
        }

        @Test
        @DisplayName("400 – missing 'file' part")
        @WithMockUser(roles = ADMIN)
        void upload_missingPart() throws Exception {
            // NOTE: with the current GlobalExceptionHandler, this returns 500 (handled by handleGenericException).
            // This test fails until a handler for MissingServletRequestPartException is added.
            mockMvc.perform(multipart("/admin/banners/uploads"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(bannerService);
        }
    }

    @Nested
    @DisplayName("POST /admin/banners")
    class CreateBanner {

        @Test
        @DisplayName("201 – creates a banner with one item")
        @WithMockUser(roles = ADMIN)
        void create_success() throws Exception {
            UUID bannerId = UUID.randomUUID();
            when(bannerService.createBanner(any())).thenReturn(
                    new BannerResponse(bannerId, true, Instant.now(), List.of()));

            mockMvc.perform(post("/admin/banners")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isActive\": true, \"items\": [" + itemJson(1) + "]}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Banner created"))
                    .andExpect(jsonPath("$.data.id").value(bannerId.toString()));

            verify(bannerService).createBanner(argThat(r -> r.items().size() == 1));
        }

        @Test
        @DisplayName("400 – items is empty")
        @WithMockUser(roles = ADMIN)
        void create_emptyItems() throws Exception {
            mockMvc.perform(post("/admin/banners")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"items\": []}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.data[0].field").value("items"));

            verifyNoInteractions(bannerService);
        }

        @Test
        @DisplayName("400 – items is missing")
        @WithMockUser(roles = ADMIN)
        void create_missingItems() throws Exception {
            mockMvc.perform(post("/admin/banners")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isActive\": true}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data[0].field").value("items"));

            verifyNoInteractions(bannerService);
        }

        @Test
        @DisplayName("400 – more than 20 items")
        @WithMockUser(roles = ADMIN)
        void create_tooManyItems() throws Exception {
            String items = String.join(",", Collections.nCopies(21, itemJson(1)));

            mockMvc.perform(post("/admin/banners")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"items\": [" + items + "]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data[0].field").value("items"));

            verifyNoInteractions(bannerService);
        }

        @Test
        @DisplayName("400 – item has no background source")
        @WithMockUser(roles = ADMIN)
        void create_itemWithoutBackground() throws Exception {
            String invalidItem = """
                    { "sortOrder": 1, "showTitle": false, "showDescription": false, "showButton": false }
                    """;

            mockMvc.perform(post("/admin/banners")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"items\": [" + invalidItem + "]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data[?(@.message=='At least one background source must be provided')]").exists());

            verifyNoInteractions(bannerService);
        }

        @Test
        @DisplayName("400 – malformed JSON")
        @WithMockUser(roles = ADMIN)
        void create_malformedJson() throws Exception {
            mockMvc.perform(post("/admin/banners")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ esto no es json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid request body. Please check JSON format."));

            verifyNoInteractions(bannerService);
        }
    }

    @Nested
    @DisplayName("PATCH /admin/banners/{bannerId}/status")
    class UpdateBannerStatus {

        @Test
        @DisplayName("204 – deactivates a banner")
        @WithMockUser(roles = ADMIN)
        void updateStatus_success() throws Exception {
            UUID bannerId = UUID.randomUUID();

            mockMvc.perform(patch("/admin/banners/{id}/status", bannerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"isActive\": false }"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(bannerService).updateBannerStatus(bannerId, false);
        }

        @Test
        @DisplayName("400 – malformed body")
        @WithMockUser(roles = ADMIN)
        void updateStatus_malformedBody() throws Exception {
            mockMvc.perform(patch("/admin/banners/{id}/status", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ esto no es json"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(bannerService);
        }
    }

    @Nested
    @DisplayName("DELETE /admin/banners/{bannerId}")
    class DeleteBanner {

        @Test
        @DisplayName("204 – deletes the banner")
        @WithMockUser(roles = ADMIN)
        void deleteBanner_success() throws Exception {
            UUID bannerId = UUID.randomUUID();

            mockMvc.perform(delete("/admin/banners/{id}", bannerId))
                    .andExpect(status().isNoContent());

            verify(bannerService).deleteBanner(bannerId);
        }

        @Test
        @DisplayName("400 – malformed UUID")
        @WithMockUser(roles = ADMIN)
        void deleteBanner_invalidUuid() throws Exception {
            mockMvc.perform(delete("/admin/banners/{id}", "nope"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(bannerService);
        }
    }

    // =========================================================
    // BANNER ITEMS
    // =========================================================

    @Nested
    @DisplayName("POST /admin/banners/{bannerId}/items")
    class AddBannerItem {

        @Test
        @DisplayName("201 – adds an item at the end")
        @WithMockUser(roles = ADMIN)
        void add_success() throws Exception {
            UUID bannerId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            when(bannerService.addItem(eq(bannerId), any(com.artelier.api.dto.request.ItemBannerRequest.class)))
                    .thenReturn(itemResponse(itemId, "Handmade Ceramics"));

            mockMvc.perform(post("/admin/banners/{id}/items", bannerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(itemJson(1)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Item added"))
                    .andExpect(jsonPath("$.data.id").value(itemId.toString()))
                    .andExpect(jsonPath("$.data.title").value("Handmade Ceramics"));
        }

        @Test
        @DisplayName("400 – sortOrder is less than 1")
        @WithMockUser(roles = ADMIN)
        void add_invalidSortOrder() throws Exception {
            mockMvc.perform(post("/admin/banners/{id}/items", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(itemJson(0)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data[0].field").value("sortOrder"));

            verifyNoInteractions(bannerService);
        }

        @Test
        @DisplayName("400 – showTitle is enabled without a title")
        @WithMockUser(roles = ADMIN)
        void add_titleRequired() throws Exception {
            String json = """
                    {
                      "sortOrder": 1,
                      "showTitle": true,
                      "backgroundImageUrl": "https://res.cloudinary.com/demo/image/upload/banner.jpg",
                      "showDescription": false,
                      "showButton": false
                    }
                    """;

            mockMvc.perform(post("/admin/banners/{id}/items", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data[?(@.message=='A title is required when showTitle is enabled')]").exists());

            verifyNoInteractions(bannerService);
        }
    }

    @Nested
    @DisplayName("POST /admin/banners/{bannerId}/items/{order}")
    class InsertBannerItem {

        @Test
        @DisplayName("201 – inserts an item at the specified position")
        @WithMockUser(roles = ADMIN)
        void insert_success() throws Exception {
            UUID bannerId = UUID.randomUUID();
            when(bannerService.addItem(eq(bannerId), any(com.artelier.api.dto.request.ItemBannerRequest.class), eq(2)))
                    .thenReturn(itemResponse(UUID.randomUUID(), "Handmade Ceramics"));

            mockMvc.perform(post("/admin/banners/{id}/items/{order}", bannerId, 2)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(itemJson(2)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Item added"));

            verify(bannerService).addItem(eq(bannerId), any(com.artelier.api.dto.request.ItemBannerRequest.class), eq(2));
        }

        @Test
        @DisplayName("400 – order is not numeric")
        @WithMockUser(roles = ADMIN)
        void insert_invalidOrderType() throws Exception {
            mockMvc.perform(post("/admin/banners/{id}/items/{order}", UUID.randomUUID(), "abc")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(itemJson(1)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid value for parameter 'order'"));

            verifyNoInteractions(bannerService);
        }
    }

    @Nested
    @DisplayName("PUT /admin/banners/{bannerId}/items/{itemId}")
    class UpdateBannerItem {

        @Test
        @DisplayName("200 – updates the item")
        @WithMockUser(roles = ADMIN)
        void update_success() throws Exception {
            UUID bannerId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            when(bannerService.updateItem(eq(bannerId), eq(itemId), any()))
                    .thenReturn(itemResponse(itemId, "Updated artwork"));

            mockMvc.perform(put("/admin/banners/{b}/items/{i}", bannerId, itemId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(ITEM_UPDATE_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Item updated"))
                    .andExpect(jsonPath("$.data.title").value("Updated artwork"));
        }

        @Test
        @DisplayName("400 – showButton is enabled without a label or URL")
        @WithMockUser(roles = ADMIN)
        void update_buttonRequired() throws Exception {
            String json = """
                    {
                      "title": "x",
                      "showTitle": true,
                      "backgroundImageUrl": "https://res.cloudinary.com/demo/image/upload/banner.jpg",
                      "showDescription": false,
                      "showButton": true
                    }
                    """;

            mockMvc.perform(put("/admin/banners/{b}/items/{i}", UUID.randomUUID(), UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data[?(@.message=='Button label and URL are required when showButton is enabled')]").exists());

            verifyNoInteractions(bannerService);
        }
    }

    @Nested
    @DisplayName("PATCH /admin/banners/{bannerId}/items/{itemId}/status")
    class UpdateBannerItemStatus {

        @Test
        @DisplayName("204 – activates an item")
        @WithMockUser(roles = ADMIN)
        void itemStatus_success() throws Exception {
            UUID bannerId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();

            mockMvc.perform(patch("/admin/banners/{b}/items/{i}/status", bannerId, itemId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"isActive\": true }"))
                    .andExpect(status().isNoContent());

            verify(bannerService).updateItemStatus(bannerId, itemId, true);
        }
    }

    @Nested
    @DisplayName("PATCH /admin/banners/{bannerId}/items/{itemId}/position")
    class MoveBannerItem {

        @Test
        @DisplayName("204 – moves the item to position 2")
        @WithMockUser(roles = ADMIN)
        void move_success() throws Exception {
            UUID bannerId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();

            mockMvc.perform(patch("/admin/banners/{b}/items/{i}/position", bannerId, itemId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"newOrder\": 2 }"))
                    .andExpect(status().isNoContent());

            verify(bannerService).moveItem(bannerId, itemId, 2);
        }
    }

    @Nested
    @DisplayName("DELETE /admin/banners/{bannerId}/items/{itemId}")
    class DeleteBannerItem {

        @Test
        @DisplayName("204 – deletes the item")
        @WithMockUser(roles = ADMIN)
        void deleteItem_success() throws Exception {
            UUID bannerId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();

            mockMvc.perform(delete("/admin/banners/{b}/items/{i}", bannerId, itemId))
                    .andExpect(status().isNoContent());

            verify(bannerService).deleteItem(bannerId, itemId);
        }
    }

    // =========================================================
    // CATEGORIES
    // =========================================================

    @Nested
    @DisplayName("POST /admin/categories")
    class CreateCategory {

        @Test
        @DisplayName("201 – creates a category")
        @WithMockUser(roles = ADMIN)
        void create_success() throws Exception {
            UUID id = UUID.randomUUID();
            when(categoryService.create(any())).thenReturn(
                    new CategoryResponse(id, "Ceramics", "ceramics", "Handmade ceramic products"));

            mockMvc.perform(post("/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Ceramics", "slug": "ceramics", "description": "Handmade ceramic products" }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Category created"))
                    .andExpect(jsonPath("$.data.id").value(id.toString()))
                    .andExpect(jsonPath("$.data.slug").value("ceramics"));

            verify(categoryService).create(argThat(r -> r.name().equals("Ceramics") && r.slug().equals("ceramics")));
        }

        @Test
        @DisplayName("400 – name is blank")
        @WithMockUser(roles = ADMIN)
        void create_blankName() throws Exception {
            mockMvc.perform(post("/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"name\": \"  \", \"slug\": \"ceramics\" }"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data[0].field").value("name"))
                    .andExpect(jsonPath("$.data[0].message").value("Name is required"));

            verifyNoInteractions(categoryService);
        }

        @Test
        @DisplayName("400 – slug is missing")
        @WithMockUser(roles = ADMIN)
        void create_missingSlug() throws Exception {
            mockMvc.perform(post("/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"name\": \"Ceramics\" }"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data[0].field").value("slug"))
                    .andExpect(jsonPath("$.data[0].message").value("Slug is required"));

            verifyNoInteractions(categoryService);
        }
    }

    // =========================================================
    // PRODUCTS
    // =========================================================

    @Nested
    @DisplayName("PATCH /admin/products/{id}/toggle")
    class ToggleProduct {

        @Test
        @DisplayName("200 – toggles visibility")
        @WithMockUser(roles = ADMIN)
        void toggle_success() throws Exception {
            UUID id = UUID.randomUUID();
            when(productService.toggleActive(id)).thenReturn(productResponse(id, false));

            mockMvc.perform(patch("/admin/products/{id}/toggle", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Product visibility updated"))
                    .andExpect(jsonPath("$.data.id").value(id.toString()))
                    .andExpect(jsonPath("$.data.isActive").value(false));

            verify(productService).toggleActive(id);
        }
    }

    @Nested
    @DisplayName("DELETE /admin/products/{id}")
    class DeleteProduct {

        @Test
        @DisplayName("204 – soft-deletes the product")
        @WithMockUser(roles = ADMIN)
        void delete_success() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/admin/products/{id}", id))
                    .andExpect(status().isNoContent());

            verify(productService).deleteProduct(id);
        }
    }

    @Nested
    @DisplayName("POST /admin/products")
    class CreateProduct {

        @Test
        @DisplayName("201 – creates a product with an image")
        @WithMockUser(roles = ADMIN)
        void create_withImage() throws Exception {
            UUID categoryId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            when(productService.createProduct(any(), any())).thenReturn(productResponse(productId, true));

            mockMvc.perform(multipart("/admin/products")
                            .file(dataPart(productJson(categoryId)))
                            .file(imagePart()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Product created successfully"))
                    .andExpect(jsonPath("$.data.id").value(productId.toString()))
                    .andExpect(jsonPath("$.data.slug").value("handmade-ceramic-mug"))
                    .andExpect(jsonPath("$.data.price").value(49.99));

            verify(productService).createProduct(
                    argThat(r -> r.categoryId().equals(categoryId) && r.name().equals("Handmade Ceramic Mug")),
                    argThat(images -> images != null && images.size() == 1));
        }

        @Test
        @DisplayName("201 – creates a product without images")
        @WithMockUser(roles = ADMIN)
        void create_withoutImages() throws Exception {
            when(productService.createProduct(any(), any()))
                    .thenReturn(productResponse(UUID.randomUUID(), true));

            mockMvc.perform(multipart("/admin/products")
                            .file(dataPart(productJson(UUID.randomUUID()))))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("400 – price equals zero")
        @WithMockUser(roles = ADMIN)
        void create_invalidPrice() throws Exception {
            String json = productJson(UUID.randomUUID()).replace("49.99", "0");

            mockMvc.perform(multipart("/admin/products").file(dataPart(json)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data[0].field").value("price"));

            verifyNoInteractions(productService);
        }

        @Test
        @DisplayName("400 – categoryId is missing")
        @WithMockUser(roles = ADMIN)
        void create_missingCategory() throws Exception {
            String json = """
                    {
                      "name": "Handmade Ceramic Mug",
                      "price": 49.99,
                      "stockType": "AVAILABLE",
                      "isCustomOrder": false,
                      "isActive": true
                    }
                    """;

            mockMvc.perform(multipart("/admin/products").file(dataPart(json)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data[0].field").value("categoryId"))
                    .andExpect(jsonPath("$.data[0].message").value("Category is required"));

            verifyNoInteractions(productService);
        }

        @Test
        @DisplayName("400 – invalid stockType")
        @WithMockUser(roles = ADMIN)
        void create_invalidStockType() throws Exception {
            String json = productJson(UUID.randomUUID()).replace("AVAILABLE", "NOPE");

            mockMvc.perform(multipart("/admin/products").file(dataPart(json)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(productService);
        }

        @Test
        @DisplayName("400 – missing 'data' part")
        @WithMockUser(roles = ADMIN)
        void create_missingDataPart() throws Exception {
            // Same case as the upload: this currently returns 500 until a handler exists
            // for MissingServletRequestPartException.
            mockMvc.perform(multipart("/admin/products").file(imagePart()))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(productService);
        }
    }

    @Nested
    @DisplayName("PUT /admin/products/{id}")
    class UpdateProduct {

        @Test
        @DisplayName("200 – updates a product")
        @WithMockUser(roles = ADMIN)
        void update_success() throws Exception {
            UUID id = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            when(productService.updateProduct(eq(id), any(), any())).thenReturn(productResponse(id, true));

            mockMvc.perform(multipart(HttpMethod.PUT, "/admin/products/{id}", id)
                            .file(dataPart(productJson(categoryId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Product updated successfully"))
                    .andExpect(jsonPath("$.data.id").value(id.toString()));

            verify(productService).updateProduct(eq(id), argThat(r -> r.categoryId().equals(categoryId)), any());
        }

        @Test
        @DisplayName("400 – malformed UUID")
        @WithMockUser(roles = ADMIN)
        void update_invalidUuid() throws Exception {
            mockMvc.perform(multipart(HttpMethod.PUT, "/admin/products/{id}", "nope")
                            .file(dataPart(productJson(UUID.randomUUID()))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(productService);
        }

        @Test
        @DisplayName("400 – name is blank")
        @WithMockUser(roles = ADMIN)
        void update_blankName() throws Exception {
            String json = productJson(UUID.randomUUID()).replace("Handmade Ceramic Mug", "");

            mockMvc.perform(multipart(HttpMethod.PUT, "/admin/products/{id}", UUID.randomUUID())
                            .file(dataPart(json)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data[0].field").value("name"));

            verifyNoInteractions(productService);
        }
    }
}