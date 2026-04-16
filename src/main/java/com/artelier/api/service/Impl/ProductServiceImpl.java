package com.artelier.api.service.Impl;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMapper productMapper;

    @Cacheable(value = "products", key = "#categoryId + '-' + #pageable.pageNumber")
    @Override
    public Page<ProductResponse> getAllProducts(UUID categoryId, Pageable pageable) {

        Page<Product> page;

        if (categoryId != null) {
            page = productRepository.findByCategoryId(categoryId, pageable);
        } else {
            page = productRepository.findAll(pageable);
        }

        return page.map(productMapper::toResponse);
    }

    @Override
    public ProductResponse getBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> ArtelierException.notFound("Product not found"));

        return productMapper.toResponse(product);
    }

    @CacheEvict(value = "products", allEntries = true)
    @Override
    public ProductResponse createProduct(ProductRequest request) {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> ArtelierException.notFound("Category not found"));

        Product product = Product.create(request, category, generateSlug(request.getName()));

        if (request.getImages() != null) {
            List<ProductImage> images = request.getImages().stream().map(imgReq -> {
                ProductImage img = new ProductImage();
                img.setUrl(imgReq.getUrl());
                img.setCloudinaryId(imgReq.getCloudinaryId());
                img.setIsPrimary(imgReq.getIsPrimary());
                img.setSortOrder(imgReq.getSortOrder());
                img.setProduct(product);
                return img;
            }).toList();

            product.setImages(images);
        }

        Product saved = productRepository.save(product);

        return productMapper.toResponse(saved);
    }

    @CacheEvict(value = "products", allEntries = true)
    @Override
    public ProductResponse updateProduct(UUID productId, ProductRequest request) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ArtelierException.notFound("Product not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> ArtelierException.notFound("Category not found"));

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

        Product saved = productRepository.save(product);

        return productMapper.toResponse(saved);
    }

    @CacheEvict(value = "products", allEntries = true)
    @Override
    public void deleteProduct(UUID productId) {
        Product product = productRepository.findById(productId).
                orElseThrow(() -> ArtelierException.notFound("Product not found"));

        productRepository.delete(product);
    }

    @CacheEvict(value = "products", allEntries = true)
    @Override
    public ProductResponse toggleActive(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ArtelierException.notFound("Product not found"));
        product.setIsActive(!product.getIsActive());
        return productMapper.toResponse(productRepository.save(product));
    }


    private String generateSlug(String name) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");

        String slug = base;
        int count = 1;

        while (productRepository.findBySlug(slug).isPresent()) {
            slug = base + "-" + count++;
        }

        return slug;
    }
}
