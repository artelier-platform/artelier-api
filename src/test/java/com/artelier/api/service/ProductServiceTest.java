package com.artelier.api.service;

import com.artelier.api.dto.request.ProductRequest;
import com.artelier.api.dto.response.ProductResponse;
import com.artelier.api.entity.Category;
import com.artelier.api.entity.Product;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.mapper.ProductMapper;
import com.artelier.api.repository.CategoryRepository;
import com.artelier.api.repository.ProductRepository;
import com.artelier.api.service.Impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl service;

    @Test
    void shouldReturnAllProductsWithoutCategory() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Product> page = new PageImpl<>(java.util.List.of(new Product()));

        when(productRepository.findAll(pageable)).thenReturn(page);
        when(productMapper.toResponse(any())).thenReturn(new ProductResponse());

        Page<ProductResponse> result = service.getAllProducts(null, pageable);

        assertEquals(1, result.getContent().size());
        verify(productRepository).findAll(pageable);
    }

    @Test
    void shouldReturnProductsByCategory() {
        Pageable pageable = PageRequest.of(0, 10);
        UUID categoryId = UUID.randomUUID();

        Page<Product> page = new PageImpl<>(java.util.List.of(new Product()));

        when(productRepository.findByCategoryId(categoryId, pageable)).thenReturn(page);
        when(productMapper.toResponse(any())).thenReturn(new ProductResponse());

        Page<ProductResponse> result = service.getAllProducts(categoryId, pageable);

        assertEquals(1, result.getContent().size());
        verify(productRepository).findByCategoryId(categoryId, pageable);
    }

    @Test
    void shouldReturnProductBySlug() {
        Product product = new Product();

        when(productRepository.findBySlug("test"))
                .thenReturn(Optional.of(product));

        when(productMapper.toResponse(product))
                .thenReturn(new ProductResponse());

        ProductResponse result = service.getBySlug("test");

        assertNotNull(result);
    }

    @Test
    void shouldThrowIfProductNotFoundBySlug() {
        when(productRepository.findBySlug("test"))
                .thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.getBySlug("test"));
    }

    @Test
    void shouldCreateProduct() {
        UUID categoryId = UUID.randomUUID();

        ProductRequest request = new ProductRequest();
        request.setName("Test Product");
        request.setCategoryId(categoryId);

        Category category = new Category();

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        when(productRepository.existsBySlug(any()))
                .thenReturn(false);

        when(productRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(productMapper.toResponse(any()))
                .thenReturn(new ProductResponse());

        ProductResponse result = service.createProduct(request);

        assertNotNull(result);
        verify(productRepository).save(any());
    }

    @Test
    void shouldThrowIfCategoryNotFoundOnCreate() {
        ProductRequest request = new ProductRequest();
        request.setCategoryId(UUID.randomUUID());

        when(categoryRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.createProduct(request));
    }

    @Test
    void shouldUpdateProduct() {
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Product product = new Product();
        product.setIsActive(true);

        ProductRequest request = new ProductRequest();
        request.setName("Updated");
        request.setCategoryId(categoryId);

        Category category = new Category();

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        when(productRepository.existsBySlug(any()))
                .thenReturn(false);

        when(productRepository.save(product))
                .thenReturn(product);

        when(productMapper.toResponse(product))
                .thenReturn(new ProductResponse());

        ProductResponse result = service.updateProduct(productId, request);

        assertNotNull(result);
        verify(productRepository).save(product);
    }

    @Test
    void shouldThrowIfProductNotFoundOnUpdate() {
        when(productRepository.findById(any()))
                .thenReturn(Optional.empty());

        ProductRequest request = new ProductRequest();

        assertThrows(ArtelierException.class,
                () -> service.updateProduct(UUID.randomUUID(), request));
    }

    @Test
    void shouldDeleteProduct() {
        UUID id = UUID.randomUUID();

        Product product = new Product();

        when(productRepository.findById(id))
                .thenReturn(Optional.of(product));

        service.deleteProduct(id);

        verify(productRepository).delete(product);
    }

    @Test
    void shouldThrowIfProductNotFoundOnDelete() {
        when(productRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> service.deleteProduct(UUID.randomUUID()));
    }

    @Test
    void shouldToggleProductActive() {
        UUID id = UUID.randomUUID();

        Product product = new Product();
        product.setIsActive(true);

        when(productRepository.findById(id))
                .thenReturn(Optional.of(product));

        when(productRepository.save(product))
                .thenReturn(product);

        when(productMapper.toResponse(product))
                .thenReturn(new ProductResponse());

        ProductResponse result = service.toggleActive(id);

        assertFalse(product.getIsActive());
        assertNotNull(result);
    }

}