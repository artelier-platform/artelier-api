package com.artelier.api.controller;

import com.artelier.api.config.JacksonTestConfig;
import com.artelier.api.config.SecurityConfig;
import com.artelier.api.dto.response.CategoryResponse;
import com.artelier.api.dto.response.ProductImageResponse;
import com.artelier.api.dto.response.ProductResponse;
import com.artelier.api.enums.StockType;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link ProductController}. They run with the real {@link SecurityConfig}
 * (filters enabled), so every contract test is also an anonymous-access test of the public catalog.
 */
@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, JacksonTestConfig.class})
@DisplayName("ProductController")
class ProductControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-15T10:30:00Z");
    private static final UUID CATEGORY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440111");
    private static final String SLUG = "handmade-ceramic-mug";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    // ------------------------------------------------------------------ fixtures

    private static CategoryResponse ceramics() {
        return new CategoryResponse(CATEGORY_ID, "Ceramics", "ceramics", "Handmade ceramic products");
    }

    private static ProductResponse product(UUID id, String name, String slug) {
        return new ProductResponse(
                id, ceramics(), name, slug,
                "Beautiful handmade ceramic mug", "Crafted by local artisans",
                new BigDecimal("49.99"), StockType.AVAILABLE, 10,
                false, true, CREATED_AT, List.of());
    }

    private static ProductResponse mug() {
        return product(UUID.randomUUID(), "Handmade Ceramic Mug", SLUG);
    }

    private static Page<ProductResponse> page(List<ProductResponse> content, int page, int size, long total) {
        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    private static Page<ProductResponse> emptyPage() {
        return page(List.of(), 0, 20, 0);
    }

    // ------------------------------------------------------------------ GET /products

    @Nested
    @DisplayName("GET /products")
    class GetAll {

        @Test
        @DisplayName("200 – anonymous visitor gets every product field in the standard envelope")
        void mapsAllFields() throws Exception {
            UUID id = UUID.randomUUID();
            when(productService.getAllProducts(any(), any(Pageable.class)))
                    .thenReturn(page(List.of(product(id, "Handmade Ceramic Mug", SLUG)), 0, 20, 1));

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Products retrieved"))
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].id").value(id.toString()))
                    .andExpect(jsonPath("$.data.content[0].name").value("Handmade Ceramic Mug"))
                    .andExpect(jsonPath("$.data.content[0].slug").value(SLUG))
                    .andExpect(jsonPath("$.data.content[0].description").value("Beautiful handmade ceramic mug"))
                    .andExpect(jsonPath("$.data.content[0].story").value("Crafted by local artisans"))
                    .andExpect(jsonPath("$.data.content[0].price").value(49.99))
                    .andExpect(jsonPath("$.data.content[0].stockType").value("AVAILABLE"))
                    .andExpect(jsonPath("$.data.content[0].stockQuantity").value(10))
                    .andExpect(jsonPath("$.data.content[0].isCustomOrder").value(false))
                    .andExpect(jsonPath("$.data.content[0].isActive").value(true))
                    .andExpect(jsonPath("$.data.content[0].createdAt").exists())
                    .andExpect(jsonPath("$.data.content[0].category.id").value(CATEGORY_ID.toString()))
                    .andExpect(jsonPath("$.data.content[0].category.name").value("Ceramics"))
                    .andExpect(jsonPath("$.data.content[0].category.slug").value("ceramics"))
                    .andExpect(jsonPath("$.data.content[0].images").isArray());
        }

        @Test
        @DisplayName("200 – product images are serialized with their own fields")
        void mapsImages() throws Exception {
            UUID imageId = UUID.randomUUID();
            ProductResponse withImages = new ProductResponse(
                    UUID.randomUUID(), ceramics(), "Handmade Ceramic Mug", SLUG,
                    null, null, new BigDecimal("49.99"), StockType.AVAILABLE, 10,
                    false, true, CREATED_AT,
                    List.of(new ProductImageResponse(imageId,
                            "https://res.cloudinary.com/artelier/image/upload/products/mug.jpg",
                            "products/mug_abc123", true, 0)));
            when(productService.getAllProducts(any(), any(Pageable.class)))
                    .thenReturn(page(List.of(withImages), 0, 20, 1));

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].images.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].images[0].id").value(imageId.toString()))
                    .andExpect(jsonPath("$.data.content[0].images[0].url")
                            .value("https://res.cloudinary.com/artelier/image/upload/products/mug.jpg"))
                    .andExpect(jsonPath("$.data.content[0].images[0].cloudinaryId").value("products/mug_abc123"))
                    .andExpect(jsonPath("$.data.content[0].images[0].isPrimary").value(true))
                    .andExpect(jsonPath("$.data.content[0].images[0].sortOrder").value(0));
        }

        @Test
        @DisplayName("200 – optional fields (description, story, stockQuantity) may be null")
        void optionalFieldsMayBeNull() throws Exception {
            ProductResponse minimal = new ProductResponse(
                    UUID.randomUUID(), ceramics(), "Linen Napkin", "linen-napkin",
                    null, null, new BigDecimal("12.50"), StockType.UNLIMITED, null,
                    false, true, CREATED_AT, List.of());
            when(productService.getAllProducts(any(), any(Pageable.class)))
                    .thenReturn(page(List.of(minimal), 0, 20, 1));

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].slug").value("linen-napkin"))
                    .andExpect(jsonPath("$.data.content[0].stockType").value("UNLIMITED"))
                    .andExpect(jsonPath("$.data.content[0].description").doesNotExist())
                    .andExpect(jsonPath("$.data.content[0].story").doesNotExist())
                    .andExpect(jsonPath("$.data.content[0].stockQuantity").doesNotExist());
        }

        @ParameterizedTest(name = "stockType {0} is serialized by name")
        @EnumSource(StockType.class)
        @DisplayName("200 – every stock type is exposed by its enum name")
        void stockTypes(StockType stockType) throws Exception {
            ProductResponse typed = new ProductResponse(
                    UUID.randomUUID(), ceramics(), "Any product", "any-product",
                    null, null, new BigDecimal("10.00"), stockType, 3,
                    false, true, CREATED_AT, List.of());
            when(productService.getAllProducts(any(), any(Pageable.class)))
                    .thenReturn(page(List.of(typed), 0, 20, 1));

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].stockType").value(stockType.name()));
        }

        @Test
        @DisplayName("200 – keeps the order returned by the service")
        void preservesOrdering() throws Exception {
            when(productService.getAllProducts(any(), any(Pageable.class)))
                    .thenReturn(page(List.of(
                            product(UUID.randomUUID(), "Third", "third"),
                            product(UUID.randomUUID(), "First", "first"),
                            product(UUID.randomUUID(), "Second", "second")), 0, 20, 3));

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].slug").value("third"))
                    .andExpect(jsonPath("$.data.content[1].slug").value("first"))
                    .andExpect(jsonPath("$.data.content[2].slug").value("second"));
        }

        @Test
        @DisplayName("200 – page metadata (totals, first/last, number of elements) is exposed")
        void pageMetadata() throws Exception {
            when(productService.getAllProducts(any(), any(Pageable.class)))
                    .thenReturn(page(List.of(
                            product(UUID.randomUUID(), "A", "a"),
                            product(UUID.randomUUID(), "B", "b")), 1, 2, 5));

            mockMvc.perform(get("/products").param("page", "1").param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(5))
                    .andExpect(jsonPath("$.data.totalPages").value(3))
                    .andExpect(jsonPath("$.data.number").value(1))
                    .andExpect(jsonPath("$.data.size").value(2))
                    .andExpect(jsonPath("$.data.numberOfElements").value(2))
                    .andExpect(jsonPath("$.data.first").value(false))
                    .andExpect(jsonPath("$.data.last").value(false))
                    .andExpect(jsonPath("$.data.empty").value(false));
        }

        @Test
        @DisplayName("200 – no products returns an empty page, not an error")
        void emptyPageIsNotAnError() throws Exception {
            when(productService.getAllProducts(any(), any(Pageable.class))).thenReturn(emptyPage());

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Products retrieved"))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(0))
                    .andExpect(jsonPath("$.data.empty").value(true));
        }

        @Test
        @DisplayName("200 – responds with a JSON body")
        void respondsWithJson() throws Exception {
            when(productService.getAllProducts(any(), any(Pageable.class))).thenReturn(emptyPage());

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Without parameters: no category filter and the default page (0, size 20, unsorted)")
        void defaultsWithoutParameters() throws Exception {
            when(productService.getAllProducts(any(), any(Pageable.class))).thenReturn(emptyPage());

            mockMvc.perform(get("/products")).andExpect(status().isOk());

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(productService, times(1)).getAllProducts(isNull(), pageable.capture());
            verifyNoMoreInteractions(productService);
            assertThat(pageable.getValue().getPageNumber()).isZero();
            assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
            assertThat(pageable.getValue().getSort().isUnsorted()).isTrue();
        }

        @Test
        @DisplayName("categoryId is forwarded to the service as a UUID")
        void forwardsCategoryId() throws Exception {
            UUID categoryId = UUID.randomUUID();
            when(productService.getAllProducts(eq(categoryId), any(Pageable.class))).thenReturn(emptyPage());

            mockMvc.perform(get("/products").param("categoryId", categoryId.toString()))
                    .andExpect(status().isOk());

            verify(productService).getAllProducts(eq(categoryId), any(Pageable.class));
        }

        @Test
        @DisplayName("page, size and sort are forwarded to the service")
        void forwardsPagination() throws Exception {
            when(productService.getAllProducts(any(), any(Pageable.class))).thenReturn(emptyPage());

            mockMvc.perform(get("/products")
                            .param("page", "2")
                            .param("size", "5")
                            .param("sort", "price,desc"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(productService).getAllProducts(isNull(), pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
            assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
            Sort.Order order = pageable.getValue().getSort().getOrderFor("price");
            assertThat(order).isNotNull();
            assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("400 – malformed categoryId never reaches the service and the value is not echoed")
        void malformedCategoryId() throws Exception {
            mockMvc.perform(get("/products").param("categoryId", "not-a-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid value for parameter 'categoryId'"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verifyNoInteractions(productService);
        }

        @Test
        @DisplayName("Ignores unknown query parameters")
        void ignoresUnknownParameters() throws Exception {
            when(productService.getAllProducts(any(), any(Pageable.class))).thenReturn(emptyPage());

            mockMvc.perform(get("/products").param("unknown", "value"))
                    .andExpect(status().isOk());

            verify(productService, times(1)).getAllProducts(isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("500 – unexpected failure is mapped to the generic error body")
        void unexpectedFailure() throws Exception {
            when(productService.getAllProducts(any(), any(Pageable.class)))
                    .thenThrow(new IllegalStateException("db down"));

            mockMvc.perform(get("/products"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Something went wrong"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("ArtelierException keeps its own status and message")
        void artelierExceptionIsTranslated() throws Exception {
            when(productService.getAllProducts(any(), any(Pageable.class)))
                    .thenThrow(new ArtelierException("Catalog unavailable", HttpStatus.SERVICE_UNAVAILABLE));

            mockMvc.perform(get("/products"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Catalog unavailable"));
        }
    }

    // ------------------------------------------------------------------ GET /products/{slug}

    @Nested
    @DisplayName("GET /products/{slug}")
    class GetBySlug {

        @Test
        @DisplayName("200 – anonymous visitor gets the product in the standard envelope")
        void mapsAllFields() throws Exception {
            UUID id = UUID.randomUUID();
            when(productService.getBySlug(SLUG)).thenReturn(product(id, "Handmade Ceramic Mug", SLUG));

            mockMvc.perform(get("/products/{slug}", SLUG))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Product retrieved"))
                    .andExpect(jsonPath("$.data.id").value(id.toString()))
                    .andExpect(jsonPath("$.data.name").value("Handmade Ceramic Mug"))
                    .andExpect(jsonPath("$.data.slug").value(SLUG))
                    .andExpect(jsonPath("$.data.description").value("Beautiful handmade ceramic mug"))
                    .andExpect(jsonPath("$.data.story").value("Crafted by local artisans"))
                    .andExpect(jsonPath("$.data.price").value(49.99))
                    .andExpect(jsonPath("$.data.stockType").value("AVAILABLE"))
                    .andExpect(jsonPath("$.data.stockQuantity").value(10))
                    .andExpect(jsonPath("$.data.isCustomOrder").value(false))
                    .andExpect(jsonPath("$.data.isActive").value(true))
                    .andExpect(jsonPath("$.data.createdAt").exists())
                    .andExpect(jsonPath("$.data.category.id").value(CATEGORY_ID.toString()))
                    .andExpect(jsonPath("$.data.category.slug").value("ceramics"))
                    .andExpect(jsonPath("$.data.images").isArray());
        }

        @Test
        @DisplayName("200 – responds with a JSON body")
        void respondsWithJson() throws Exception {
            when(productService.getBySlug(SLUG)).thenReturn(mug());

            mockMvc.perform(get("/products/{slug}", SLUG))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("The slug is passed to the service verbatim, exactly once")
        void delegatesSlugVerbatim() throws Exception {
            when(productService.getBySlug("Mug_2-Edition")).thenReturn(mug());

            mockMvc.perform(get("/products/{slug}", "Mug_2-Edition"))
                    .andExpect(status().isOk());

            verify(productService, times(1)).getBySlug("Mug_2-Edition");
            verifyNoMoreInteractions(productService);
        }

        @Test
        @DisplayName("404 – unknown slug returns the error envelope")
        void unknownSlug() throws Exception {
            when(productService.getBySlug("does-not-exist"))
                    .thenThrow(ArtelierException.notFound("Product not found"));

            mockMvc.perform(get("/products/{slug}", "does-not-exist"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Product not found"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("500 – unexpected failure is mapped to the generic error body")
        void unexpectedFailure() throws Exception {
            when(productService.getBySlug(SLUG)).thenThrow(new IllegalStateException("db down"));

            mockMvc.perform(get("/products/{slug}", SLUG))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Something went wrong"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    // ------------------------------------------------------------------ access rules

    @Nested
    @DisplayName("Access rules")
    class AccessRules {

        @ParameterizedTest(name = "authenticated {0} can list products")
        @ValueSource(strings = {"BUYER", "ADMIN"})
        @DisplayName("200 – authenticated users can list products")
        void authenticatedCanList(String role) throws Exception {
            when(productService.getAllProducts(any(), any(Pageable.class))).thenReturn(emptyPage());

            mockMvc.perform(get("/products").with(user("someone").roles(role)))
                    .andExpect(status().isOk());

            verify(productService).getAllProducts(isNull(), any(Pageable.class));
        }

        @ParameterizedTest(name = "authenticated {0} can read a product")
        @ValueSource(strings = {"BUYER", "ADMIN"})
        @DisplayName("200 – authenticated users can read a product")
        void authenticatedCanRead(String role) throws Exception {
            when(productService.getBySlug(SLUG)).thenReturn(mug());

            mockMvc.perform(get("/products/{slug}", SLUG).with(user("someone").roles(role)))
                    .andExpect(status().isOk());

            verify(productService).getBySlug(SLUG);
        }
    }
}
