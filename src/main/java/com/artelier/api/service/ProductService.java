package com.artelier.api.service;

import com.artelier.api.dto.request.ProductRequest;
import com.artelier.api.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    Page<ProductResponse> getAllProducts(UUID categoryId, Pageable pageable);

    ProductResponse getBySlug(String slug);

    /**
     * Creates a product and uploads the provided images to Cloudinary.
     *
     * @param request product data (name, price, stock, etc.) + image metadata (isPrimary, sortOrder)
     * @param images  binary files to upload; coordinated by index with {@code request.getImages()}
     */
    ProductResponse createProduct(ProductRequest request, List<MultipartFile> images);

    /**
     * Updates an existing product. Images are replaced only when {@code images} is non-empty.
     * Omitting the files part leaves existing images unchanged.
     *
     * @param productId target product UUID
     * @param request   updated product data + optional image metadata
     * @param images    new files to upload; pass null or empty to keep current images
     */
    ProductResponse updateProduct(UUID productId, ProductRequest request, List<MultipartFile> images);

    void deleteProduct(UUID productId);

    ProductResponse toggleActive(UUID productId);
}