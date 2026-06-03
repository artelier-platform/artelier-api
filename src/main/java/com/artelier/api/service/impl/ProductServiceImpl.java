package com.artelier.api.service.impl;

import com.artelier.api.dto.request.ProductRequest;
import com.artelier.api.dto.response.ProductResponse;
import com.artelier.api.entity.Category;
import com.artelier.api.entity.Product;
import com.artelier.api.entity.ProductImage;
import com.artelier.api.exception.ArtelierException;
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

    private final String PRODUCT_NOT_FOUND = "Product not found";
    private final String CATEGORY_NOT_FOUND = "Category not found";
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

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
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> ArtelierException.notFound(PRODUCT_NOT_FOUND));

        return productMapper.toResponse(product);
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    @Override
    public ProductResponse createProduct(ProductRequest request, List<MultipartFile> images)  {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> ArtelierException.notFound(CATEGORY_NOT_FOUND));

        Product product = Product.create(request, category, generateSlug(request.getName()));

        attachImages(product, request);

        return productMapper.toResponse(productRepository.save(product));
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    @Override
    public ProductResponse updateProduct(UUID productId, ProductRequest request) {
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

        product.getImages().clear();
        attachImages(product, request);

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


    private void attachImages(Product product, ProductRequest request) {
        if (request.getImages() == null || request.getImages().isEmpty()) {
            return;
        }

        List<ProductImage> images = request.getImages().stream()
                .map(imgReq -> {
                    ProductImage img = new ProductImage();
                    img.setUrl(imgReq.getUrl());
                    img.setCloudinaryId(imgReq.getCloudinaryId());
                    img.setIsPrimary(imgReq.getIsPrimary());
                    img.setSortOrder(imgReq.getSortOrder());
                    img.setProduct(product);
                    return img;
                })
                .toList();

        product.getImages().addAll(images);
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