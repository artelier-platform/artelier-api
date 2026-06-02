package com.artelier.api.controller;

import com.artelier.api.config.JacksonTestConfig;
import com.artelier.api.dto.request.ProductRequest;
import com.artelier.api.dto.response.ProductResponse;
import com.artelier.api.enums.StockType;
import com.artelier.api.security.JwtUtil;
import com.artelier.api.integration.cloudinary.service.CloudinaryService;
import com.artelier.api.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.Map;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonTestConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CloudinaryService cloudinaryService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    // 🔹 GET ALL
    @Test
    void shouldGetAllProducts() throws Exception {
        ProductResponse product = new ProductResponse();
        product.setName("Test Product");

        when(productService.getAllProducts(isNull(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(product)));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Test Product"));
    }

    // 🔹 GET BY SLUG
    @Test
    void shouldGetProductBySlug() throws Exception {
        ProductResponse product = new ProductResponse();
        product.setName("Slug Product");

        when(productService.getBySlug("test-slug")).thenReturn(product);

        mockMvc.perform(get("/products/test-slug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Slug Product"));
    }

    // 🔹 CREATE
    @Test
    void shouldCreateProduct() throws Exception {
        ProductRequest request = buildRequest();

        ProductResponse response = new ProductResponse();
        response.setName("Created Product");

        when(productService.createProduct(request)).thenReturn(response);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Created Product"));
    }

    // 🔹 UPDATE
    @Test
    void shouldUpdateProduct() throws Exception {
        UUID id = UUID.randomUUID();
        ProductRequest request = buildRequest();

        ProductResponse response = new ProductResponse();
        response.setName("Updated Product");

        when(productService.updateProduct(id, request)).thenReturn(response);

        mockMvc.perform(put("/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Product"));
    }

    // 🔹 DELETE
    @Test
    void shouldDeleteProduct() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product deleted"));
    }

    // 🔹 TOGGLE ACTIVE
    @Test
    void shouldToggleActive() throws Exception {
        UUID id = UUID.randomUUID();

        ProductResponse response = new ProductResponse();
        response.setIsActive(false);

        when(productService.toggleActive(id)).thenReturn(response);

        mockMvc.perform(patch("/products/" + id + "/toggle-active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // 🔹 UPLOAD
    @Test
    void shouldUploadImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes()
        );

        when(cloudinaryService.upload(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of("url", "https://cloudinary.com/image.jpg"));

        mockMvc.perform(multipart("/products/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://cloudinary.com/image.jpg"));
    }

    // 🔧 helper
    private ProductRequest buildRequest() {
        ProductRequest request = new ProductRequest();
        request.setCategoryId(UUID.randomUUID());
        request.setName("Test Product");
        request.setDescription("desc");
        request.setPrice(BigDecimal.valueOf(10));
        request.setStockType(StockType.AVAILABLE);
        request.setStockQuantity(5);
        request.setIsCustomOrder(false);
        request.setIsActive(true);
        return request;
    }
}