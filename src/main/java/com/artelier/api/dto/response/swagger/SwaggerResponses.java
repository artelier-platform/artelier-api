package com.artelier.api.dto.response.swagger;

import com.artelier.api.dto.response.*;
import com.artelier.api.integration.wompi.dto.response.WompiFinancialInstitutionsResponse;
import org.springframework.data.domain.Page;
import java.util.List;

/**
 * Concrete wrapper classes for SpringDoc/OpenAPI schema generation.
 * AppResponse<T> uses a generic that OpenAPI cannot resolve at runtime.
 * These subclasses fix that by binding T to a concrete type, so
 * openapi-typescript generates fully-typed responses instead of `data: unknown`.
 * Usage: @Schema(implementation = SwaggerResponses.AuthResponseBody.class)
 * Never instantiate these directly — they exist only for Swagger documentation.
 */
public final class SwaggerResponses {

    private SwaggerResponses() {}

    // ─── Auth ────────────────────────────────────────────────────────────────
    public static class AuthResponseBody extends AppResponse<AuthResponse> {
        public AuthResponseBody() { super(true, "", null); }
    }

    // ─── Categories ──────────────────────────────────────────────────────────
    public static class CategoryResponseBody extends AppResponse<CategoryResponse> {
        public CategoryResponseBody() { super(true, "", null); }
    }

    public static class CategoryListResponseBody extends AppResponse<List<CategoryResponse>> {
        public CategoryListResponseBody() { super(true, "", null); }
    }

    // ─── Products ────────────────────────────────────────────────────────────
    public static class ProductResponseBody extends AppResponse<ProductResponse> {
        public ProductResponseBody() { super(true, "", null); }
    }

    public static class ProductPageResponseBody extends AppResponse<Page<ProductResponse>> {
        public ProductPageResponseBody() { super(true, "", null); }
    }

    // ─── Orders ──────────────────────────────────────────────────────────────
    public static class OrderResponseBody extends AppResponse<OrderResponse> {
        public OrderResponseBody() { super(true, "", null); }
    }

    public static class OrderListResponseBody extends AppResponse<List<OrderResponse>> {
        public OrderListResponseBody() { super(true, "", null); }
    }

    public static class OrderPageResponseBody extends AppResponse<Page<OrderResponse>> {
        public OrderPageResponseBody() { super(true, "", null); }
    }

    // ─── Payments ────────────────────────────────────────────────────────────
    public static class PaymentResponseBody extends AppResponse<PaymentResponse> {
        public PaymentResponseBody() { super(true, "", null); }
    }

    public static class FinancialInstitutionsResponseBody extends AppResponse<List<WompiFinancialInstitutionsResponse>> {
        public FinancialInstitutionsResponseBody() { super(true, "", null); }
    }

    // ─── Stats ───────────────────────────────────────────────────────────────
    public static class StatsResponseBody extends AppResponse<StatsResponse> {
        public StatsResponseBody() { super(true, "", null); }
    }
}