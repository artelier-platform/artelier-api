package com.artelier.api.service.impl;

import com.artelier.api.dto.request.ImageMetadata;
import com.artelier.api.dto.request.ProductRequest;
import com.artelier.api.dto.response.ProductResponse;
import com.artelier.api.entity.Category;
import com.artelier.api.entity.Product;
import com.artelier.api.entity.ProductImage;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.integration.cloudinary.dto.response.CloudinaryUploadResponse;
import com.artelier.api.integration.cloudinary.service.CloudinaryService;
import com.artelier.api.mapper.ProductMapper;
import com.artelier.api.repository.CategoryRepository;
import com.artelier.api.repository.ProductRepository;
import com.artelier.api.service.ProductService;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {

    // static final: these are constants, not injectable dependencies.
    // @AllArgsConstructor excludes fields with inline initializers, but
    // declaring them static makes the intent unambiguous to SonarCloud and readers.
    private static final String PRODUCT_NOT_FOUND = "Product not found";
    private static final String CATEGORY_NOT_FOUND = "Category not found";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CloudinaryService cloudinaryService;

    // ─── Reads ────────────────────────────────────────────────────────────────

    @Cacheable(
            value = "products",
            key = "(#categoryId != null ? #categoryId : 'all') + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort"
    )
    @Transactional(readOnly = true)
    @Override
    public Page<ProductResponse> getAllProducts(UUID categoryId, Pageable pageable) {
        Page<Product> page = (categoryId != null)
                ? productRepository.findByCategoryId(categoryId, pageable)
                : productRepository.findAll(pageable);

        return page.map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public ProductResponse getBySlug(String slug) {
        return productMapper.toResponse(
                productRepository.findBySlug(slug)
                        .orElseThrow(() -> ArtelierException.notFound(PRODUCT_NOT_FOUND))
        );
    }

    // ─── Writes ───────────────────────────────────────────────────────────────

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    @Override
    public ProductResponse createProduct(ProductRequest request, List<MultipartFile> images) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> ArtelierException.notFound(CATEGORY_NOT_FOUND));

        Product product = Product.create(request, category, generateSlug(request.getName()));

        attachImages(product, images, request.getImages());

        return productMapper.toResponse(productRepository.save(product));
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    @Override
    public ProductResponse updateProduct(UUID productId, ProductRequest request, List<MultipartFile> images) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ArtelierException.notFound(PRODUCT_NOT_FOUND));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> ArtelierException.notFound(CATEGORY_NOT_FOUND));

        product.setName(request.getName());
        product.setSlug(generateSlug(request.getName()));
        product.setDescription(request.getDescription());
        product.setStory(request.getStory());
        product.setPrice(request.getPrice());
        product.setStockType(request.getStockType());
        product.setStockQuantity(request.getStockQuantity());
        product.setIsCustomOrder(request.getIsCustomOrder());
        product.setIsActive(request.getIsActive());
        product.setCategory(category);

        if (images != null && !images.isEmpty()) {
            product.getImages().clear();
            attachImages(product, images, request.getImages());
        }

        return productMapper.toResponse(productRepository.save(product));
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    @Override
    public void deleteProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ArtelierException.notFound(PRODUCT_NOT_FOUND));

        product.setDeletedAt(Instant.now());
        productRepository.save(product);
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    @Override
    public ProductResponse toggleActive(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ArtelierException.notFound(PRODUCT_NOT_FOUND));

        product.setIsActive(!product.getIsActive());

        return productMapper.toResponse(productRepository.save(product));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Uploads each file to Cloudinary and attaches the resulting images to the product.
     *
     * <p>Files and metadata are coordinated by index: {@code files.get(i)} corresponds
     * to {@code metadata.get(i)}. If {@code metadata} is null, shorter than {@code files},
     * or missing at a given index, safe defaults are applied:
     * <ul>
     *   <li>The first image defaults to {@code isPrimary = true}.</li>
     *   <li>{@code sortOrder} defaults to the file's index position.</li>
     * </ul>
     *
     * @param product  the product to attach images to
     * @param files    multipart files uploaded by the client
     * @param metadata display metadata (isPrimary, sortOrder) sent alongside the files
     */
    private void attachImages(Product product, List<MultipartFile> files, List<ImageMetadata> metadata) {
        if (files == null || files.isEmpty()) return;

        for (int i = 0; i < files.size(); i++) {
            CloudinaryUploadResponse uploaded = cloudinaryService.upload(files.get(i));

            boolean isPrimary;
            int sortOrder;

            if (metadata != null && i < metadata.size()) {
                ImageMetadata meta = metadata.get(i);
                isPrimary = Boolean.TRUE.equals(meta.getIsPrimary());
                sortOrder = meta.getSortOrder() != null ? meta.getSortOrder() : i;
            } else {
                isPrimary = (i == 0);
                sortOrder = i;
            }

            ProductImage img = new ProductImage();
            img.setUrl(uploaded.getSecureUrl());
            img.setCloudinaryId(uploaded.getPublicId());
            img.setIsPrimary(isPrimary);
            img.setSortOrder(sortOrder);
            img.setProduct(product);
            product.getImages().add(img);
        }
    }

    private String generateSlug(String name) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");

        if (!productRepository.existsBySlug(base)) {
            return base;
        }

        int count = 1;
        String candidate;
        do {
            candidate = base + "-" + count++;
        } while (productRepository.existsBySlug(candidate));

        return candidate;
    }
}