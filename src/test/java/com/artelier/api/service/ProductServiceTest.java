package com.artelier.api.service;

import com.artelier.api.dto.request.ImageMetadata;
import com.artelier.api.dto.request.ProductRequest;
import com.artelier.api.dto.response.ProductResponse;
import com.artelier.api.entity.Category;
import com.artelier.api.entity.Product;
import com.artelier.api.entity.ProductImage;
import com.artelier.api.enums.StockType;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.integration.cloudinary.dto.response.CloudinaryUploadResponse;
import com.artelier.api.integration.cloudinary.service.CloudinaryService;
import com.artelier.api.mapper.ProductMapper;
import com.artelier.api.repository.CategoryRepository;
import com.artelier.api.repository.ProductRepository;
import com.artelier.api.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProductServiceImpl}. Repositories, mapper and Cloudinary are mocked,
 * so no Spring context, cache or database is involved.
 *
 * <p>Not covered here (needs a Spring context): {@code @Cacheable} / {@code @CacheEvict}
 * behaviour and {@code @Transactional} rollback.</p>
 *
 * <p>The nested class {@code CurrentBehaviorToReview} pins behaviour that works today but is
 * questionable. Those tests are NOT endorsements: if the production code is improved,
 * update or delete them.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl")
class ProductServiceTest {

    private static final String PRODUCT_NOT_FOUND = "Product not found";
    private static final String CATEGORY_NOT_FOUND = "Category not found";

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CloudinaryService cloudinaryService;

    private ProductServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(categoryRepository, productRepository, productMapper, cloudinaryService);
    }

    // =========================
    // HELPERS
    // =========================

    private static Category category(UUID id) {
        Category category = new Category();
        category.setId(id);
        category.setName("Ceramics");
        category.setSlug("ceramics");
        return category;
    }

    private static Product product(String name, String slug, boolean active) {
        return Product.builder()
                .id(UUID.randomUUID())
                .name(name)
                .slug(slug)
                .description("old description")
                .story("old story")
                .price(new BigDecimal("10.00"))
                .stockType(StockType.AVAILABLE)
                .stockQuantity(3)
                .isCustomOrder(false)
                .isActive(active)
                .category(category(UUID.randomUUID()))
                .build();
    }

    private static ProductImage image(Product owner, String cloudinaryId) {
        ProductImage image = new ProductImage();
        image.setUrl("https://res.cloudinary.com/artelier/image/upload/" + cloudinaryId + ".png");
        image.setCloudinaryId(cloudinaryId);
        image.setIsPrimary(true);
        image.setSortOrder(0);
        image.setProduct(owner);
        return image;
    }

    private static ProductRequest request(UUID categoryId, String name, List<ImageMetadata> images) {
        return new ProductRequest(
                categoryId, name, "Description", "Story",
                new BigDecimal("49.99"), StockType.AVAILABLE, 10, false, true, images);
    }

    private static ProductResponse response(String name) {
        return new ProductResponse(
                UUID.randomUUID(), null, name, name, null, null,
                BigDecimal.TEN, StockType.AVAILABLE, 1, false, true,
                Instant.now(), List.of());
    }

    private static MultipartFile file(String name) {
        return new MockMultipartFile("images", name + ".png", "image/png", new byte[]{1, 2, 3});
    }

    private static CloudinaryUploadResponse uploaded(String publicId) {
        CloudinaryUploadResponse uploaded = new CloudinaryUploadResponse();
        uploaded.setPublicId(publicId);
        uploaded.setSecureUrl("https://res.cloudinary.com/artelier/image/upload/" + publicId + ".png");
        return uploaded;
    }

    private static List<Boolean> primaries(Product product) {
        return product.getImages().stream().map(ProductImage::getIsPrimary).toList();
    }

    private static List<Integer> sortOrders(Product product) {
        return product.getImages().stream().map(ProductImage::getSortOrder).toList();
    }

    private static void assertNotFound(ArtelierException exception, String message) {
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals(message, exception.getMessage());
    }

    private void stubUpload(MultipartFile file, String publicId) {
        when(cloudinaryService.upload(file)).thenReturn(uploaded(publicId));
    }

    private Category stubCategory(UUID id) {
        Category category = category(id);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        return category;
    }

    /** save() returns the entity it receives and the mapper returns a fixed response. */
    private ProductResponse stubSaveAndMap() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        ProductResponse expected = response("saved");
        when(productMapper.toResponse(any(Product.class))).thenReturn(expected);
        return expected;
    }

    private Product savedProduct() {
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        return captor.getValue();
    }

    /** Creates a product with no images and returns what the service handed to save(). */
    private Product createAndCapture(String name) {
        UUID categoryId = UUID.randomUUID();
        stubCategory(categoryId);
        stubSaveAndMap();

        service.createProduct(request(categoryId, name, null), null);

        return savedProduct();
    }

    // Slug sources live in the outer class so @MethodSource can reference them from nested classes.

    static Stream<Arguments> normalizedSlugCases() {
        return Stream.of(
                arguments("Handmade Ceramic Mug", "handmade-ceramic-mug"),
                arguments("Vase 2000", "vase-2000"),
                arguments("UPPER lower MiXeD", "upper-lower-mixed"),
                arguments("Mug    Azul", "mug-azul"),
                arguments("tabs\tand\nnewlines", "tabs-and-newlines"),
                arguments("Taza (50% off)!", "taza-50-off"),
                arguments("blue-mug", "blue-mug")
        );
    }

    static Stream<Arguments> slugQuirkCases() {
        return Stream.of(
                // accented letters are dropped, not transliterated
                arguments("Cer\u00e1mica Artesanal", "cermica-artesanal"),
                arguments("Ni\u00f1o Feliz", "nio-feliz"),
                // no trim: leading / trailing whitespace becomes hyphens
                arguments("  Mug Azul  ", "-mug-azul-"),
                // each space around an existing hyphen becomes its own hyphen
                arguments("Mug - Azul", "mug---azul"),
                // names made only of symbols produce an empty slug
                arguments("!!!", ""),
                arguments("@#$%", "")
        );
    }

    // =========================
    // getAllProducts
    // =========================

    @Nested
    @DisplayName("getAllProducts(categoryId, pageable)")
    class GetAllProducts {

        private final Pageable pageable = PageRequest.of(1, 5, Sort.by("name"));

        @Test
        @DisplayName("with categoryId it filters by category, maps every item and keeps the paging data")
        void filtersByCategory() {
            UUID categoryId = UUID.randomUUID();
            Product a = product("A", "a", true);
            Product b = product("B", "b", true);
            ProductResponse responseA = response("a");
            ProductResponse responseB = response("b");

            when(productRepository.findByCategoryId(categoryId, pageable))
                    .thenReturn(new PageImpl<>(List.of(a, b), pageable, 12));
            when(productMapper.toResponse(a)).thenReturn(responseA);
            when(productMapper.toResponse(b)).thenReturn(responseB);

            Page<ProductResponse> result = service.getAllProducts(categoryId, pageable);

            assertEquals(List.of(responseA, responseB), result.getContent());
            assertEquals(12L, result.getTotalElements());
            assertEquals(1, result.getNumber());
            assertEquals(5, result.getSize());
            verify(productRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("without categoryId it lists every product and never filters by category")
        void listsAll() {
            Product a = product("A", "a", true);
            ProductResponse responseA = response("a");

            when(productRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(a), pageable, 6));
            when(productMapper.toResponse(a)).thenReturn(responseA);

            Page<ProductResponse> result = service.getAllProducts(null, pageable);

            assertEquals(List.of(responseA), result.getContent());
            verify(productRepository, never()).findByCategoryId(any(), any());
        }

        @Test
        @DisplayName("an empty page is returned as is and the mapper is never called")
        void emptyPage() {
            when(productRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

            Page<ProductResponse> result = service.getAllProducts(null, pageable);

            assertTrue(result.isEmpty());
            assertEquals(0L, result.getTotalElements());
            verifyNoInteractions(productMapper);
        }

        @Test
        @DisplayName("filtering by a category that does not exist returns an empty page (the category is never checked)")
        void unknownCategoryReturnsEmptyPage() {
            UUID unknownCategory = UUID.randomUUID();
            when(productRepository.findByCategoryId(unknownCategory, pageable)).thenReturn(Page.empty(pageable));

            Page<ProductResponse> result = service.getAllProducts(unknownCategory, pageable);

            assertTrue(result.isEmpty());
            verifyNoInteractions(categoryRepository, productMapper);
        }

        @Test
        @DisplayName("a repository failure is propagated untouched")
        void repositoryFailureIsPropagated() {
            when(productRepository.findAll(pageable)).thenThrow(new DataAccessResourceFailureException("db down"));

            assertThrows(DataAccessResourceFailureException.class, () -> service.getAllProducts(null, pageable));

            verifyNoInteractions(productMapper);
        }

        @Test
        @DisplayName("a mapper failure on one element is not swallowed")
        void mapperFailureIsPropagated() {
            Product broken = product("Broken", "broken", true);
            when(productRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(broken), pageable, 6));
            when(productMapper.toResponse(broken)).thenThrow(new IllegalStateException("cannot map"));

            assertThrows(IllegalStateException.class, () -> service.getAllProducts(null, pageable));
        }
    }

    // =========================
    // getBySlug
    // =========================

    @Nested
    @DisplayName("getBySlug(slug)")
    class GetBySlug {

        @Test
        @DisplayName("returns the mapped product when the slug exists")
        void found() {
            Product mug = product("Mug", "mug", true);
            ProductResponse expected = response("mug");

            when(productRepository.findBySlug("mug")).thenReturn(Optional.of(mug));
            when(productMapper.toResponse(mug)).thenReturn(expected);

            assertSame(expected, service.getBySlug("mug"));
        }

        @Test
        @DisplayName("404 - unknown slug: throws not found and the mapper is never called")
        void notFound() {
            when(productRepository.findBySlug("ghost")).thenReturn(Optional.empty());

            ArtelierException exception = assertThrows(ArtelierException.class, () -> service.getBySlug("ghost"));

            assertNotFound(exception, PRODUCT_NOT_FOUND);
            verifyNoInteractions(productMapper);
        }

        @ParameterizedTest(name = "slug=[{0}] is passed as-is and ends in 404")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "MUG", "does-not-exist"})
        @DisplayName("null, blank or differently-cased slugs are not normalized and end in 404")
        void oddSlugsAreNotNormalized(String slug) {
            ArtelierException exception = assertThrows(ArtelierException.class, () -> service.getBySlug(slug));

            assertNotFound(exception, PRODUCT_NOT_FOUND);
            verify(productRepository).findBySlug(slug);
            verifyNoInteractions(productMapper);
        }
    }

    // =========================
    // createProduct
    // =========================

    @Nested
    @DisplayName("createProduct(request, images)")
    class CreateProduct {

        @Test
        @DisplayName("404 - unknown category: nothing is saved, mapped or uploaded")
        void categoryNotFound() {
            UUID categoryId = UUID.randomUUID();
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
            List<MultipartFile> files = List.of(file("a"));

            ArtelierException exception = assertThrows(ArtelierException.class,
                    () -> service.createProduct(request(categoryId, "Mug", null), files));

            assertNotFound(exception, CATEGORY_NOT_FOUND);
            verifyNoInteractions(productRepository, productMapper, cloudinaryService);
        }

        @Test
        @DisplayName("builds the product from the request, saves it and maps the SAVED entity")
        void createsProduct() {
            UUID categoryId = UUID.randomUUID();
            Category category = stubCategory(categoryId);
            Product persisted = product("Handmade Mug", "handmade-mug", true);
            ProductResponse expected = response("handmade-mug");

            when(productRepository.save(any(Product.class))).thenReturn(persisted);
            when(productMapper.toResponse(persisted)).thenReturn(expected);

            ProductResponse result = service.createProduct(
                    new ProductRequest(categoryId, "Handmade Mug", "A mug", "Crafted by hand",
                            new BigDecimal("49.99"), StockType.MADE_TO_ORDER, 7, true, false, null),
                    null);

            assertSame(expected, result);

            Product saved = savedProduct();
            assertNull(saved.getId());
            assertEquals("Handmade Mug", saved.getName());
            assertEquals("handmade-mug", saved.getSlug());
            assertEquals("A mug", saved.getDescription());
            assertEquals("Crafted by hand", saved.getStory());
            assertEquals(new BigDecimal("49.99"), saved.getPrice());
            assertEquals(StockType.MADE_TO_ORDER, saved.getStockType());
            assertEquals(7, saved.getStockQuantity());
            assertTrue(saved.getIsCustomOrder());
            assertFalse(saved.getIsActive());
            assertSame(category, saved.getCategory());
            assertTrue(saved.getImages().isEmpty());
            verifyNoInteractions(cloudinaryService);
        }

        @Test
        @DisplayName("optional fields left null stay null (UNLIMITED stock without quantity)")
        void optionalFieldsStayNull() {
            UUID categoryId = UUID.randomUUID();
            stubCategory(categoryId);
            stubSaveAndMap();

            service.createProduct(
                    new ProductRequest(categoryId, "Mug", null, null,
                            new BigDecimal("1.00"), StockType.UNLIMITED, null, false, true, null),
                    null);

            Product saved = savedProduct();
            assertNull(saved.getDescription());
            assertNull(saved.getStory());
            assertNull(saved.getStockQuantity());
            assertEquals(StockType.UNLIMITED, saved.getStockType());
        }

        @Test
        @DisplayName("an empty files list uploads nothing and attaches no images")
        void emptyFilesList() {
            UUID categoryId = UUID.randomUUID();
            stubCategory(categoryId);
            stubSaveAndMap();

            service.createProduct(request(categoryId, "Mug", List.of(new ImageMetadata(true, 0))), List.of());

            assertTrue(savedProduct().getImages().isEmpty());
            verifyNoInteractions(cloudinaryService);
        }

        @Test
        @DisplayName("metadata without files: nothing is uploaded")
        void metadataWithoutFiles() {
            UUID categoryId = UUID.randomUUID();
            stubCategory(categoryId);
            stubSaveAndMap();

            service.createProduct(
                    request(categoryId, "Mug", List.of(new ImageMetadata(true, 0), new ImageMetadata(false, 1))),
                    null);

            assertTrue(savedProduct().getImages().isEmpty());
            verifyNoInteractions(cloudinaryService);
        }

        @Test
        @DisplayName("uploads every file in order and applies its metadata by index")
        void appliesMetadataByIndex() {
            UUID categoryId = UUID.randomUUID();
            stubCategory(categoryId);
            stubSaveAndMap();
            MultipartFile first = file("first");
            MultipartFile second = file("second");
            stubUpload(first, "artelier/products/first");
            stubUpload(second, "artelier/products/second");

            service.createProduct(
                    request(categoryId, "Mug", List.of(new ImageMetadata(false, 7), new ImageMetadata(true, 3))),
                    List.of(first, second));

            Product saved = savedProduct();
            assertEquals(2, saved.getImages().size());

            ProductImage firstImage = saved.getImages().get(0);
            assertEquals("artelier/products/first", firstImage.getCloudinaryId());
            assertEquals("https://res.cloudinary.com/artelier/image/upload/artelier/products/first.png", firstImage.getUrl());
            assertFalse(firstImage.getIsPrimary());
            assertEquals(7, firstImage.getSortOrder());
            assertSame(saved, firstImage.getProduct());

            ProductImage secondImage = saved.getImages().get(1);
            assertEquals("artelier/products/second", secondImage.getCloudinaryId());
            assertTrue(secondImage.getIsPrimary());
            assertEquals(3, secondImage.getSortOrder());
            assertSame(saved, secondImage.getProduct());

            InOrder order = inOrder(cloudinaryService);
            order.verify(cloudinaryService).upload(first);
            order.verify(cloudinaryService).upload(second);
        }

        @Test
        @DisplayName("without metadata (null) the first image is primary and sortOrder is the index")
        void defaultsWhenMetadataIsNull() {
            UUID categoryId = UUID.randomUUID();
            stubCategory(categoryId);
            stubSaveAndMap();
            MultipartFile a = file("a");
            MultipartFile b = file("b");
            MultipartFile c = file("c");
            stubUpload(a, "a");
            stubUpload(b, "b");
            stubUpload(c, "c");

            service.createProduct(request(categoryId, "Mug", null), List.of(a, b, c));

            Product saved = savedProduct();
            assertEquals(List.of(true, false, false), primaries(saved));
            assertEquals(List.of(0, 1, 2), sortOrders(saved));
        }

        @Test
        @DisplayName("an empty metadata list behaves like missing metadata")
        void defaultsWhenMetadataIsEmpty() {
            UUID categoryId = UUID.randomUUID();
            stubCategory(categoryId);
            stubSaveAndMap();
            MultipartFile a = file("a");
            MultipartFile b = file("b");
            stubUpload(a, "a");
            stubUpload(b, "b");

            service.createProduct(request(categoryId, "Mug", List.of()), List.of(a, b));

            Product saved = savedProduct();
            assertEquals(List.of(true, false), primaries(saved));
            assertEquals(List.of(0, 1), sortOrders(saved));
        }

        @Test
        @DisplayName("metadata shorter than the files: the uncovered files fall back to defaults")
        void metadataShorterThanFiles() {
            UUID categoryId = UUID.randomUUID();
            stubCategory(categoryId);
            stubSaveAndMap();
            MultipartFile a = file("a");
            MultipartFile b = file("b");
            MultipartFile c = file("c");
            stubUpload(a, "a");
            stubUpload(b, "b");
            stubUpload(c, "c");

            service.createProduct(request(categoryId, "Mug", List.of(new ImageMetadata(true, 5))), List.of(a, b, c));

            Product saved = savedProduct();
            assertEquals(List.of(true, false, false), primaries(saved));
            assertEquals(List.of(5, 1, 2), sortOrders(saved));
        }

        @Test
        @DisplayName("metadata longer than the files: the extra entries are ignored and nothing fails")
        void metadataLongerThanFiles() {
            UUID categoryId = UUID.randomUUID();
            stubCategory(categoryId);
            stubSaveAndMap();
            MultipartFile only = file("only");
            stubUpload(only, "only");

            service.createProduct(
                    request(categoryId, "Mug",
                            List.of(new ImageMetadata(true, 0), new ImageMetadata(false, 1), new ImageMetadata(false, 2))),
                    List.of(only));

            Product saved = savedProduct();
            assertEquals(1, saved.getImages().size());
            verify(cloudinaryService, times(1)).upload(any(MultipartFile.class));
        }

        @Test
        @DisplayName("a metadata entry with null isPrimary / sortOrder: isPrimary=false, sortOrder falls back to the index")
        void metadataEntryWithNullFields() {
            UUID categoryId = UUID.randomUUID();
            stubCategory(categoryId);
            stubSaveAndMap();
            MultipartFile a = file("a");
            MultipartFile b = file("b");
            stubUpload(a, "a");
            stubUpload(b, "b");

            service.createProduct(
                    request(categoryId, "Mug", List.of(new ImageMetadata(null, null), new ImageMetadata(null, null))),
                    List.of(a, b));

            Product saved = savedProduct();
            assertEquals(List.of(false, false), primaries(saved));
            assertEquals(List.of(0, 1), sortOrders(saved));
        }

        @Test
        @DisplayName("Cloudinary fails on the 2nd file: the error is propagated, the 3rd file is not uploaded and nothing is saved")
        void cloudinaryFailsMidway() {
            UUID categoryId = UUID.randomUUID();
            stubCategory(categoryId);
            MultipartFile a = file("a");
            MultipartFile b = file("b");
            MultipartFile c = file("c");
            stubUpload(a, "a");
            when(cloudinaryService.upload(b)).thenThrow(new IllegalStateException("cloudinary down"));

            assertThrows(IllegalStateException.class,
                    () -> service.createProduct(request(categoryId, "Mug", null), List.of(a, b, c)));

            verify(cloudinaryService, never()).upload(c);
            verify(productRepository, never()).save(any(Product.class));
            verifyNoInteractions(productMapper);
        }

        @Test
        @DisplayName("a null name fails before any upload: the service relies on upstream validation but must not leave orphan files")
        void nullNameFailsBeforeUpload() {
            UUID categoryId = UUID.randomUUID();
            stubCategory(categoryId);
            List<MultipartFile> files = List.of(file("a"));

            assertThrows(NullPointerException.class,
                    () -> service.createProduct(request(categoryId, null, null), files));

            verifyNoInteractions(cloudinaryService, productMapper);
            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("a failure while saving is propagated and the mapper is never called")
        void saveFailureIsPropagated() {
            UUID categoryId = UUID.randomUUID();
            stubCategory(categoryId);
            when(productRepository.save(any(Product.class))).thenThrow(new DataAccessResourceFailureException("db down"));

            assertThrows(DataAccessResourceFailureException.class,
                    () -> service.createProduct(request(categoryId, "Mug", null), null));

            verifyNoInteractions(productMapper);
        }
    }

    // =========================
    // slug generation (through createProduct)
    // =========================

    @Nested
    @DisplayName("slug generation")
    class SlugGeneration {

        @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
        @MethodSource("com.artelier.api.service.ProductServiceTest#normalizedSlugCases")
        @DisplayName("lower-cases the name, strips symbols and joins words with hyphens")
        void normalizesName(String name, String expectedSlug) {
            Product saved = createAndCapture(name);

            assertEquals(expectedSlug, saved.getSlug());
            verify(productRepository).existsBySlug(expectedSlug);
        }

        @Test
        @DisplayName("when the slug is taken it appends -1")
        void singleCollision() {
            when(productRepository.existsBySlug("mug")).thenReturn(true);
            when(productRepository.existsBySlug("mug-1")).thenReturn(false);

            Product saved = createAndCapture("Mug");

            assertEquals("mug-1", saved.getSlug());
        }

        @Test
        @DisplayName("when several suffixed slugs are taken it keeps counting until one is free")
        void manyCollisions() {
            Set<String> taken = Set.of("mug", "mug-1", "mug-2", "mug-3", "mug-4");
            when(productRepository.existsBySlug(anyString())).thenAnswer(inv -> taken.contains(inv.<String>getArgument(0)));

            Product saved = createAndCapture("Mug");

            assertEquals("mug-5", saved.getSlug());
            verify(productRepository, times(6)).existsBySlug(anyString());
        }
    }

    // =========================
    // updateProduct
    // =========================

    @Nested
    @DisplayName("updateProduct(productId, request, images)")
    class UpdateProduct {

        private final UUID productId = UUID.randomUUID();
        private final UUID categoryId = UUID.randomUUID();
        private Product existing;

        @BeforeEach
        void setUpExisting() {
            existing = product("Old name", "old-name", true);
            existing.getImages().add(image(existing, "artelier/products/old-1"));
            existing.getImages().add(image(existing, "artelier/products/old-2"));
        }

        @Test
        @DisplayName("404 - unknown product: the category is never looked up and nothing is saved")
        void productNotFound() {
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            ArtelierException exception = assertThrows(ArtelierException.class,
                    () -> service.updateProduct(productId, request(categoryId, "New", null), null));

            assertNotFound(exception, PRODUCT_NOT_FOUND);
            verifyNoInteractions(categoryRepository, cloudinaryService, productMapper);
            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("404 - unknown category: the product stays untouched, nothing is uploaded or saved")
        void categoryNotFound() {
            when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
            List<MultipartFile> files = List.of(file("new"));

            ArtelierException exception = assertThrows(ArtelierException.class,
                    () -> service.updateProduct(productId, request(categoryId, "New name", null), files));

            assertNotFound(exception, CATEGORY_NOT_FOUND);
            assertEquals("Old name", existing.getName());
            assertEquals("old-name", existing.getSlug());
            assertEquals(2, existing.getImages().size());
            verify(productRepository, never()).existsBySlug(any());
            verify(productRepository, never()).save(any(Product.class));
            verifyNoInteractions(cloudinaryService, productMapper);
        }

        @Test
        @DisplayName("overwrites every field, regenerates the slug and keeps the current images when no files are sent")
        void updatesFieldsAndKeepsImages() {
            Category newCategory = stubCategory(categoryId);
            when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
            ProductResponse expected = stubSaveAndMap();
            List<ProductImage> imagesBefore = List.copyOf(existing.getImages());

            ProductResponse result = service.updateProduct(
                    productId,
                    new ProductRequest(categoryId, "New Name", "New desc", "New story",
                            new BigDecimal("75.50"), StockType.MADE_TO_ORDER, 4, true, false, null),
                    null);

            assertSame(expected, result);
            assertEquals("New Name", existing.getName());
            assertEquals("new-name", existing.getSlug());
            assertEquals("New desc", existing.getDescription());
            assertEquals("New story", existing.getStory());
            assertEquals(new BigDecimal("75.50"), existing.getPrice());
            assertEquals(StockType.MADE_TO_ORDER, existing.getStockType());
            assertEquals(4, existing.getStockQuantity());
            assertTrue(existing.getIsCustomOrder());
            assertFalse(existing.getIsActive());
            assertSame(newCategory, existing.getCategory());
            assertEquals(imagesBefore, existing.getImages());
            verifyNoInteractions(cloudinaryService);
            verify(productRepository).save(existing);
        }

        @Test
        @DisplayName("optional fields sent as null overwrite the stored values")
        void optionalFieldsOverwrittenWithNull() {
            stubCategory(categoryId);
            when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
            stubSaveAndMap();

            service.updateProduct(
                    productId,
                    new ProductRequest(categoryId, "Mug", null, null,
                            new BigDecimal("1.00"), StockType.UNLIMITED, null, false, true, null),
                    null);

            assertNull(existing.getDescription());
            assertNull(existing.getStory());
            assertNull(existing.getStockQuantity());
            assertEquals(StockType.UNLIMITED, existing.getStockType());
        }

        @Test
        @DisplayName("an empty files list is treated like no files: the current images are kept")
        void emptyFilesKeepImages() {
            stubCategory(categoryId);
            when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
            stubSaveAndMap();

            service.updateProduct(productId, request(categoryId, "Mug", List.of(new ImageMetadata(true, 0))), List.of());

            assertEquals(2, existing.getImages().size());
            verifyNoInteractions(cloudinaryService);
        }

        @Test
        @DisplayName("sending files replaces the old images with the new ones, using the metadata")
        void replacesImages() {
            stubCategory(categoryId);
            when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
            stubSaveAndMap();
            MultipartFile newFile = file("new");
            stubUpload(newFile, "artelier/products/new");

            service.updateProduct(
                    productId,
                    request(categoryId, "Mug", List.of(new ImageMetadata(true, 2))),
                    List.of(newFile));

            assertEquals(1, existing.getImages().size());
            ProductImage image = existing.getImages().get(0);
            assertEquals("artelier/products/new", image.getCloudinaryId());
            assertTrue(image.getIsPrimary());
            assertEquals(2, image.getSortOrder());
            assertSame(existing, image.getProduct());
            assertTrue(existing.getImages().stream()
                    .noneMatch(i -> i.getCloudinaryId().startsWith("artelier/products/old")));
        }

        @Test
        @DisplayName("replacing images without metadata: the first new image is primary, sortOrder is the index")
        void replacesImagesWithDefaults() {
            stubCategory(categoryId);
            when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
            stubSaveAndMap();
            MultipartFile a = file("a");
            MultipartFile b = file("b");
            stubUpload(a, "a");
            stubUpload(b, "b");

            service.updateProduct(productId, request(categoryId, "Mug", null), List.of(a, b));

            assertEquals(List.of(true, false), primaries(existing));
            assertEquals(List.of(0, 1), sortOrders(existing));
        }

        @Test
        @DisplayName("Cloudinary failure while replacing images: the error is propagated and nothing is saved")
        void cloudinaryFailsDuringUpdate() {
            stubCategory(categoryId);
            when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
            MultipartFile broken = file("broken");
            when(cloudinaryService.upload(broken)).thenThrow(new IllegalStateException("cloudinary down"));
            List<MultipartFile> files = List.of(broken);

            assertThrows(IllegalStateException.class,
                    () -> service.updateProduct(productId, request(categoryId, "Mug", null), files));

            verify(productRepository, never()).save(any(Product.class));
            verifyNoInteractions(productMapper);
        }

        @Test
        @DisplayName("a failure while saving is propagated and the mapper is never called")
        void saveFailureIsPropagated() {
            stubCategory(categoryId);
            when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
            when(productRepository.save(existing)).thenThrow(new OptimisticLockingFailureException("stale"));

            assertThrows(OptimisticLockingFailureException.class,
                    () -> service.updateProduct(productId, request(categoryId, "Mug", null), null));

            verifyNoInteractions(productMapper);
        }
    }

    // =========================
    // deleteProduct
    // =========================

    @Nested
    @DisplayName("deleteProduct(productId)")
    class DeleteProduct {

        @Test
        @DisplayName("404 - unknown (or already soft-deleted) product: nothing is saved")
        void notFound() {
            UUID productId = UUID.randomUUID();
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            ArtelierException exception = assertThrows(ArtelierException.class, () -> service.deleteProduct(productId));

            assertNotFound(exception, PRODUCT_NOT_FOUND);
            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("soft delete: stamps deletedAt, saves the product and never physically deletes it")
        void softDeletes() {
            Product mug = product("Mug", "mug", true);
            when(productRepository.findById(mug.getId())).thenReturn(Optional.of(mug));

            Instant before = Instant.now();
            service.deleteProduct(mug.getId());
            Instant after = Instant.now();

            assertNotNull(mug.getDeletedAt());
            assertFalse(mug.getDeletedAt().isBefore(before));
            assertFalse(mug.getDeletedAt().isAfter(after));
            verify(productRepository).save(mug);
            verify(productRepository, never()).delete(any(Product.class));
            verify(productRepository, never()).deleteById(any());
            verifyNoInteractions(cloudinaryService, productMapper);
        }

        @Test
        @DisplayName("a failure while saving is propagated")
        void saveFailureIsPropagated() {
            Product mug = product("Mug", "mug", true);
            when(productRepository.findById(mug.getId())).thenReturn(Optional.of(mug));
            when(productRepository.save(mug)).thenThrow(new OptimisticLockingFailureException("stale"));

            assertThrows(OptimisticLockingFailureException.class, () -> service.deleteProduct(mug.getId()));
        }
    }

    // =========================
    // toggleActive
    // =========================

    @Nested
    @DisplayName("toggleActive(productId)")
    class ToggleActive {

        @Test
        @DisplayName("404 - unknown product: nothing is saved or mapped")
        void notFound() {
            UUID productId = UUID.randomUUID();
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            ArtelierException exception = assertThrows(ArtelierException.class, () -> service.toggleActive(productId));

            assertNotFound(exception, PRODUCT_NOT_FOUND);
            verify(productRepository, never()).save(any(Product.class));
            verifyNoInteractions(productMapper);
        }

        @ParameterizedTest(name = "isActive {0} -> {1}")
        @CsvSource({"true,false", "false,true"})
        @DisplayName("flips the flag, saves the product and returns the mapped result")
        void flipsTheFlag(boolean before, boolean after) {
            Product mug = product("Mug", "mug", before);
            when(productRepository.findById(mug.getId())).thenReturn(Optional.of(mug));
            ProductResponse expected = stubSaveAndMap();

            ProductResponse result = service.toggleActive(mug.getId());

            assertSame(expected, result);
            assertEquals(after, mug.getIsActive());
            verify(productRepository).save(mug);
            assertEquals("Mug", mug.getName());
        }

        @Test
        @DisplayName("toggling twice returns the product to its original state")
        void togglingTwiceRestoresState() {
            Product mug = product("Mug", "mug", true);
            when(productRepository.findById(mug.getId())).thenReturn(Optional.of(mug));
            stubSaveAndMap();

            service.toggleActive(mug.getId());
            service.toggleActive(mug.getId());

            assertTrue(mug.getIsActive());
            verify(productRepository, times(2)).save(mug);
        }

        @Test
        @DisplayName("a failure while saving is propagated and the mapper is never called")
        void saveFailureIsPropagated() {
            Product mug = product("Mug", "mug", true);
            when(productRepository.findById(mug.getId())).thenReturn(Optional.of(mug));
            when(productRepository.save(mug)).thenThrow(new OptimisticLockingFailureException("stale"));

            assertThrows(OptimisticLockingFailureException.class, () -> service.toggleActive(mug.getId()));

            verifyNoInteractions(productMapper);
        }
    }

    // =========================
    // Behaviour that works today but deserves a second look.
    // These tests document the CURRENT behaviour; they are not endorsements.
    // =========================

    @Nested
    @DisplayName("CurrentBehaviorToReview")
    class CurrentBehaviorToReview {

        @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
        @MethodSource("com.artelier.api.service.ProductServiceTest#slugQuirkCases")
        @DisplayName("slug quirks: accents dropped, no trim, repeated hyphens, empty slug for symbol-only names")
        void slugQuirks(String name, String expectedSlug) {
            Product saved = createAndCapture(name);

            assertEquals(expectedSlug, saved.getSlug());
        }

        @Test
        @DisplayName("several images flagged as primary are stored as sent (no single-primary rule)")
        void keepsMultiplePrimaryImages() {
            UUID categoryId = UUID.randomUUID();
            stubCategory(categoryId);
            stubSaveAndMap();
            MultipartFile a = file("a");
            MultipartFile b = file("b");
            stubUpload(a, "a");
            stubUpload(b, "b");

            service.createProduct(
                    request(categoryId, "Mug", List.of(new ImageMetadata(true, 0), new ImageMetadata(true, 1))),
                    List.of(a, b));

            assertEquals(List.of(true, true), primaries(savedProduct()));
        }

        @Test
        @DisplayName("a product can be created with images but none of them primary")
        void allowsProductWithoutPrimaryImage() {
            UUID categoryId = UUID.randomUUID();
            stubCategory(categoryId);
            stubSaveAndMap();
            MultipartFile a = file("a");
            MultipartFile b = file("b");
            stubUpload(a, "a");
            stubUpload(b, "b");

            service.createProduct(
                    request(categoryId, "Mug", List.of(new ImageMetadata(false, 0), new ImageMetadata(false, 1))),
                    List.of(a, b));

            assertEquals(List.of(false, false), primaries(savedProduct()));
        }

        @Test
        @DisplayName("updating a product with its own unchanged name turns its slug into 'name-1' (it collides with itself)")
        void sameNameChangesSlugOnUpdate() {
            UUID categoryId = UUID.randomUUID();
            Product mug = product("Mug", "mug", true);
            when(productRepository.findById(mug.getId())).thenReturn(Optional.of(mug));
            stubCategory(categoryId);
            stubSaveAndMap();
            when(productRepository.existsBySlug("mug")).thenReturn(true);
            when(productRepository.existsBySlug("mug-1")).thenReturn(false);

            service.updateProduct(mug.getId(), request(categoryId, "Mug", null), null);

            assertEquals("mug-1", mug.getSlug());
        }

        @Test
        @DisplayName("replacing images never deletes the previous files from Cloudinary")
        void replacedImagesAreNotDeletedFromCloudinary() {
            UUID categoryId = UUID.randomUUID();
            Product mug = product("Mug", "mug", true);
            mug.getImages().add(image(mug, "artelier/products/old"));
            when(productRepository.findById(mug.getId())).thenReturn(Optional.of(mug));
            stubCategory(categoryId);
            stubSaveAndMap();
            MultipartFile newFile = file("new");
            stubUpload(newFile, "artelier/products/new");

            service.updateProduct(mug.getId(), request(categoryId, "Mug", null), List.of(newFile));

            verify(cloudinaryService, never()).delete(anyString(), anyString());
        }
    }
}