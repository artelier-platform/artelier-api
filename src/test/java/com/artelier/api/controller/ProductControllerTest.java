package com.artelier.api.controller;

import com.artelier.api.config.JacksonTestConfig;
import com.artelier.api.dto.request.ProductRequest;
import com.artelier.api.dto.response.ProductResponse;
import com.artelier.api.enums.StockType;
import com.artelier.api.repository.UserRepository;
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

import static org.mockito.ArgumentMatchers.*;
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

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private ProductRequest buildRequest() {
        ProductRequest request = new ProductRequest();
        request.setCategoryId(UUID.randomUUID());
        request.setName("Test Product");
        request.setDescription("desc");
        request.setPrice(BigDecimal.valueOf(49.99));
        request.setStockType(StockType.AVAILABLE);
        request.setStockQuantity(10);
        request.setIsCustomOrder(false);
        request.setIsActive(true);
        return request;
    }

    private MockMultipartFile datapart(ProductRequest request) throws Exception {
        return new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );
    }

    private MockMultipartFile imagePart() {
        return new MockMultipartFile(
                "images", "image.jpg",
                MediaType.IMAGE_JPEG_VALUE, "fake-image-bytes".getBytes()
        );
    }

    // ─────────────────────────────────────────────
    // GET /products
    // ─────────────────────────────────────────────

    @Test
    void shouldGetAllProductsWithoutFilter() throws Exception {
        ProductResponse product = new ProductResponse();
        product.setName("Test Product");

        when(productService.getAllProducts(isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(product)));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Test Product"));
    }

    @Test
    void shouldGetAllProductsFilteredByCategory() throws Exception {
        UUID categoryId = UUID.randomUUID();
        ProductResponse product = new ProductResponse();
        product.setName("Ceramic Mug");

        when(productService.getAllProducts(eq(categoryId), any()))
                .thenReturn(new PageImpl<>(List.of(product)));

        mockMvc.perform(get("/products").param("categoryId", categoryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Ceramic Mug"));
    }

    // ─────────────────────────────────────────────
    // GET /products/{slug}
    // ─────────────────────────────────────────────

    @Test
    void shouldGetProductBySlug() throws Exception {
        ProductResponse product = new ProductResponse();
        product.setName("Slug Product");

        when(productService.getBySlug("test-slug")).thenReturn(product);

        mockMvc.perform(get("/products/test-slug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product retrieved"))
                .andExpect(jsonPath("$.data.name").value("Slug Product"));
    }

    // ─────────────────────────────────────────────
    // POST /products — multipart/form-data
    // ─────────────────────────────────────────────

    @Test
    void shouldCreateProductWithImages() throws Exception {
        ProductRequest request = buildRequest();
        ProductResponse response = new ProductResponse();
        response.setName("Created Product");

        when(productService.createProduct(any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/products")
                        .file(datapart(request))
                        .file(imagePart()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product created successfully"))
                .andExpect(jsonPath("$.data.name").value("Created Product"));
    }

    @Test
    void shouldCreateProductWithoutImages() throws Exception {
        ProductRequest request = buildRequest();
        ProductResponse response = new ProductResponse();
        response.setName("Created Product");

        when(productService.createProduct(any(), isNull())).thenReturn(response);

        mockMvc.perform(multipart("/products")
                        .file(datapart(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Created Product"));
    }

    // ─────────────────────────────────────────────
    // PUT /products/{id} — multipart/form-data
    // ─────────────────────────────────────────────

    @Test
    void shouldUpdateProductWithImages() throws Exception {
        UUID id = UUID.randomUUID();
        ProductRequest request = buildRequest();
        ProductResponse response = new ProductResponse();
        response.setName("Updated Product");

        when(productService.updateProduct(eq(id), any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/products/" + id)
                        .file(datapart(request))
                        .file(imagePart())
                        .with(req -> { req.setMethod("PUT"); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Updated Product"));
    }

    @Test
    void shouldUpdateProductWithoutImages() throws Exception {
        UUID id = UUID.randomUUID();
        ProductRequest request = buildRequest();
        ProductResponse response = new ProductResponse();
        response.setName("Updated Product");

        when(productService.updateProduct(eq(id), any(), isNull())).thenReturn(response);

        mockMvc.perform(multipart("/products/" + id)
                        .file(datapart(request))
                        .with(req -> { req.setMethod("PUT"); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Product"));
    }

    // ─────────────────────────────────────────────
    // DELETE /products/{id}
    // ─────────────────────────────────────────────

    @Test
    void shouldDeleteProduct() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/products/" + id))
                .andExpect(status().isNoContent());
    }

    // ─────────────────────────────────────────────
    // PATCH /products/{id}/toggle
    // ─────────────────────────────────────────────

    @Test
    void shouldToggleProductVisibilityToFalse() throws Exception {
        UUID id = UUID.randomUUID();
        ProductResponse response = new ProductResponse();
        response.setIsActive(false);

        when(productService.toggleActive(id)).thenReturn(response);

        mockMvc.perform(patch("/products/" + id + "/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product visibility updated"))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    void shouldToggleProductVisibilityToTrue() throws Exception {
        UUID id = UUID.randomUUID();
        ProductResponse response = new ProductResponse();
        response.setIsActive(true);

        when(productService.toggleActive(id)).thenReturn(response);

        mockMvc.perform(patch("/products/" + id + "/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(true));
    }
}