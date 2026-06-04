package com.artelier.api.service;

import com.artelier.api.dto.request.ImageMetadata;
import com.artelier.api.dto.request.ProductRequest;
import com.artelier.api.dto.response.ProductResponse;
import com.artelier.api.entity.Category;
import com.artelier.api.entity.Product;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.integration.cloudinary.dto.response.CloudinaryUploadResponse;
import com.artelier.api.integration.cloudinary.service.CloudinaryService;
import com.artelier.api.mapper.ProductMapper;
import com.artelier.api.repository.CategoryRepository;
import com.artelier.api.repository.ProductRepository;
import com.artelier.api.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductMapper productMapper;
    @Mock private CloudinaryService cloudinaryService;

    @InjectMocks
    private ProductServiceImpl service;

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private ProductRequest buildRequest(UUID categoryId) {
        ProductRequest request = new ProductRequest();
        request.setCategoryId(categoryId);
        request.setName("Test Product");
        request.setDescription("desc");
        request.setPrice(BigDecimal.valueOf(49.99));
        request.setStockType(com.artelier.api.enums.StockType.AVAILABLE);
        request.setStockQuantity(10);
        request.setIsCustomOrder(false);
        request.setIsActive(true);
        return request;
    }

    private MockMultipartFile mockImage() {
        return new MockMultipartFile(
                "images", "image.jpg",
                "image/jpeg", "fake-image-bytes".getBytes()
        );
    }

    private CloudinaryUploadResponse mockUploadResponse() {
        CloudinaryUploadResponse response = new CloudinaryUploadResponse();
        response.setSecureUrl("https://res.cloudinary.com/artelier/image/upload/abc.jpg");
        response.setPublicId("artelier/products/abc");
        return response;
    }

    // ─────────────────────────────────────────────
    // getAllProducts
    // ─────────────────────────────────────────────

    @Test
    void shouldReturnAllProductsWithoutCategoryFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(new Product()));

        when(productRepository.findAll(pageable)).thenReturn(page);
        when(productMapper.toResponse(any())).thenReturn(new ProductResponse());

        Page<ProductResponse> result = service.getAllProducts(null, pageable);

        assertEquals(1, result.getContent().size());
        verify(productRepository).findAll(pageable);
        verify(productRepository, never()).findByCategoryId(any(), any());
    }

    @Test
    void shouldReturnProductsFilteredByCategory() {
        Pageable pageable = PageRequest.of(0, 10);
        UUID categoryId = UUID.randomUUID();
        Page<Product> page = new PageImpl<>(List.of(new Product()));

        when(productRepository.findByCategoryId(categoryId, pageable)).thenReturn(page);
        when(productMapper.toResponse(any())).thenReturn(new ProductResponse());

        Page<ProductResponse> result = service.getAllProducts(categoryId, pageable);

        assertEquals(1, result.getContent().size());
        verify(productRepository).findByCategoryId(categoryId, pageable);
        verify(productRepository, never()).findAll(pageable);
    }

    // ─────────────────────────────────────────────
    // getBySlug
    // ─────────────────────────────────────────────

    @Test
    void shouldReturnProductBySlug() {
        Product product = new Product();

        when(productRepository.findBySlug("handmade-mug")).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(new ProductResponse());

        ProductResponse result = service.getBySlug("handmade-mug");

        assertNotNull(result);
        verify(productRepository).findBySlug("handmade-mug");
    }

    @Test
    void shouldThrowIfProductNotFoundBySlug() {
        when(productRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class, () -> service.getBySlug("nonexistent"));
    }

    // ─────────────────────────────────────────────
    // createProduct
    // ─────────────────────────────────────────────

    @Test
    void shouldCreateProductWithoutImages() {
        UUID categoryId = UUID.randomUUID();
        ProductRequest request = buildRequest(categoryId);
        Category category = new Category();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.existsBySlug(any())).thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productMapper.toResponse(any())).thenReturn(new ProductResponse());

        ProductResponse result = service.createProduct(request, null);

        assertNotNull(result);
        verify(productRepository).save(any());
        verify(cloudinaryService, never()).upload(any());
    }

    @Test
    void shouldCreateProductWithImages() {
        UUID categoryId = UUID.randomUUID();
        ProductRequest request = buildRequest(categoryId);
        Category category = new Category();
        MultipartFile image = mockImage();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.existsBySlug(any())).thenReturn(false);
        when(cloudinaryService.upload(image)).thenReturn(mockUploadResponse());
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productMapper.toResponse(any())).thenReturn(new ProductResponse());

        ProductResponse result = service.createProduct(request, List.of(image));

        assertNotNull(result);
        verify(cloudinaryService).upload(image);
    }

    @Test
    void shouldCreateProductWithImagesAndMetadata() {
        UUID categoryId = UUID.randomUUID();
        ProductRequest request = buildRequest(categoryId);

        ImageMetadata meta = new ImageMetadata();
        meta.setIsPrimary(true);
        meta.setSortOrder(0);
        request.setImages(List.of(meta));

        Category category = new Category();
        MultipartFile image = mockImage();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.existsBySlug(any())).thenReturn(false);
        when(cloudinaryService.upload(image)).thenReturn(mockUploadResponse());
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product saved = inv.getArgument(0);
            assertFalse(saved.getImages().isEmpty());
            assertTrue(saved.getImages().getFirst().getIsPrimary());
            assertEquals(0, saved.getImages().getFirst().getSortOrder());
            return saved;
        });
        when(productMapper.toResponse(any())).thenReturn(new ProductResponse());

        service.createProduct(request, List.of(image));

        verify(cloudinaryService).upload(image);
    }

    @Test
    void shouldApplyDefaultMetadataWhenNotProvided() {
        UUID categoryId = UUID.randomUUID();
        ProductRequest request = buildRequest(categoryId);
        // no metadata set on request
        Category category = new Category();
        MultipartFile image = mockImage();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.existsBySlug(any())).thenReturn(false);
        when(cloudinaryService.upload(image)).thenReturn(mockUploadResponse());
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product saved = inv.getArgument(0);
            // first image defaults to isPrimary=true, sortOrder=0
            assertTrue(saved.getImages().getFirst().getIsPrimary());
            assertEquals(0, saved.getImages().getFirst().getSortOrder());
            return saved;
        });
        when(productMapper.toResponse(any())).thenReturn(new ProductResponse());

        service.createProduct(request, List.of(image));
    }

    @Test
    void shouldThrowIfCategoryNotFoundOnCreate() {
        ProductRequest request = buildRequest(UUID.randomUUID());

        when(categoryRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class, () -> service.createProduct(request, null));
    }

    // ─────────────────────────────────────────────
    // createProduct — slug generation
    // ─────────────────────────────────────────────

    @Test
    void shouldGenerateSlugFromName() {
        UUID categoryId = UUID.randomUUID();
        ProductRequest request = buildRequest(categoryId);
        request.setName("Handmade Ceramic Mug");
        Category category = new Category();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.existsBySlug("handmade-ceramic-mug")).thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product saved = inv.getArgument(0);
            assertEquals("handmade-ceramic-mug", saved.getSlug());
            return saved;
        });
        when(productMapper.toResponse(any())).thenReturn(new ProductResponse());

        service.createProduct(request, null);
    }

    @Test
    void shouldAppendSuffixIfSlugAlreadyExists() {
        UUID categoryId = UUID.randomUUID();
        ProductRequest request = buildRequest(categoryId);
        request.setName("Handmade Ceramic Mug");
        Category category = new Category();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.existsBySlug("handmade-ceramic-mug")).thenReturn(true);
        when(productRepository.existsBySlug("handmade-ceramic-mug-1")).thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product saved = inv.getArgument(0);
            assertEquals("handmade-ceramic-mug-1", saved.getSlug());
            return saved;
        });
        when(productMapper.toResponse(any())).thenReturn(new ProductResponse());

        service.createProduct(request, null);
    }

    @Test
    void shouldIncrementSuffixUntilSlugIsUnique() {
        UUID categoryId = UUID.randomUUID();
        ProductRequest request = buildRequest(categoryId);
        request.setName("Handmade Ceramic Mug");
        Category category = new Category();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.existsBySlug("handmade-ceramic-mug")).thenReturn(true);
        when(productRepository.existsBySlug("handmade-ceramic-mug-1")).thenReturn(true);
        when(productRepository.existsBySlug("handmade-ceramic-mug-2")).thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product saved = inv.getArgument(0);
            assertEquals("handmade-ceramic-mug-2", saved.getSlug());
            return saved;
        });
        when(productMapper.toResponse(any())).thenReturn(new ProductResponse());

        service.createProduct(request, null);
    }

    // ─────────────────────────────────────────────
    // updateProduct
    // ─────────────────────────────────────────────

    @Test
    void shouldUpdateProductWithoutReplacingImages() {
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ProductRequest request = buildRequest(categoryId);
        request.setName("Updated Name");

        Product product = new Product();
        product.setIsActive(true);
        product.setImages(new java.util.ArrayList<>());

        Category category = new Category();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.existsBySlug(any())).thenReturn(false);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(new ProductResponse());

        // no images passed — existing images preserved
        ProductResponse result = service.updateProduct(productId, request, null);

        assertNotNull(result);
        verify(cloudinaryService, never()).upload(any());
        verify(productRepository).save(product);
    }

    @Test
    void shouldUpdateProductAndReplaceImages() {
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ProductRequest request = buildRequest(categoryId);
        MultipartFile image = mockImage();

        Product product = new Product();
        product.setIsActive(true);
        product.setImages(new java.util.ArrayList<>());

        Category category = new Category();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.existsBySlug(any())).thenReturn(false);
        when(cloudinaryService.upload(image)).thenReturn(mockUploadResponse());
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(new ProductResponse());

        ProductResponse result = service.updateProduct(productId, request, List.of(image));

        assertNotNull(result);
        verify(cloudinaryService).upload(image);
        assertFalse(product.getImages().isEmpty());
    }

    @Test
    void shouldThrowIfProductNotFoundOnUpdate() {
        when(productRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.updateProduct(UUID.randomUUID(), buildRequest(UUID.randomUUID()), null));
    }

    @Test
    void shouldThrowIfCategoryNotFoundOnUpdate() {
        UUID productId = UUID.randomUUID();
        ProductRequest request = buildRequest(UUID.randomUUID());

        Product product = new Product();
        product.setImages(new java.util.ArrayList<>());

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.updateProduct(productId, request, null));
    }

    // ─────────────────────────────────────────────
    // deleteProduct
    // ─────────────────────────────────────────────

    @Test
    void shouldSoftDeleteProduct() {
        UUID id = UUID.randomUUID();
        Product product = new Product();

        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        service.deleteProduct(id);

        assertNotNull(product.getDeletedAt());
        verify(productRepository).save(product);
    }

    @Test
    void shouldThrowIfProductNotFoundOnDelete() {
        when(productRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.deleteProduct(UUID.randomUUID()));
    }

    // ─────────────────────────────────────────────
    // toggleActive
    // ─────────────────────────────────────────────

    @Test
    void shouldToggleActiveFromTrueToFalse() {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        product.setIsActive(true);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(new ProductResponse());

        service.toggleActive(id);

        assertFalse(product.getIsActive());
    }

    @Test
    void shouldToggleActiveFromFalseToTrue() {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        product.setIsActive(false);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(new ProductResponse());

        service.toggleActive(id);

        assertTrue(product.getIsActive());
    }

    @Test
    void shouldThrowIfProductNotFoundOnToggle() {
        when(productRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.toggleActive(UUID.randomUUID()));
    }
}