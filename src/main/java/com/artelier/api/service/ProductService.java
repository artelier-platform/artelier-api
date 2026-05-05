package com.artelier.api.service;

import com.artelier.api.dto.request.ProductRequest;
import com.artelier.api.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;


public interface ProductService {
    Page<ProductResponse> getAllProducts(UUID categoryId, Pageable pageable);
    ProductResponse getBySlug(String slug);
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(UUID productId, ProductRequest request);
    void deleteProduct(UUID productId);
    ProductResponse toggleActive(UUID productId);
}
