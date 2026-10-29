
package com.artelier.api.dto.response.swagger;

import com.artelier.api.dto.response.AppResponse;
import com.artelier.api.dto.response.AuthResponse;
import com.artelier.api.dto.response.BannerResponse;
import com.artelier.api.dto.response.CategoryResponse;
import com.artelier.api.dto.response.ItemBannerResponse;
import com.artelier.api.dto.response.OrderResponse;
import com.artelier.api.dto.response.PaymentResponse;
import com.artelier.api.dto.response.ProductResponse;
import com.artelier.api.dto.response.StatsResponse;
import com.artelier.api.dto.response.UploadedAssetResponse;
import com.artelier.api.integration.wompi.dto.response.WompiFinancialInstitutionsResponse;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Concrete response wrappers for SpringDoc/OpenAPI schema generation.
 *
 * AppResponse is generic, so these classes bind its type parameter to
 * concrete response types for API documentation.
 */
public final class SwaggerResponses {

    private SwaggerResponses() {
    }

    // Authentication

    public static class AuthResponseBody
            extends AppResponse<AuthResponse> {

        public AuthResponseBody() {
            super(true, "", null);
        }
    }

    // Categories

    public static class CategoryResponseBody
            extends AppResponse<CategoryResponse> {

        public CategoryResponseBody() {
            super(true, "", null);
        }
    }

    public static class CategoryListResponseBody
            extends AppResponse<List<CategoryResponse>> {

        public CategoryListResponseBody() {
            super(true, "", null);
        }
    }

    // Products

    public static class ProductResponseBody
            extends AppResponse<ProductResponse> {

        public ProductResponseBody() {
            super(true, "", null);
        }
    }

    public static class ProductPageResponseBody
            extends AppResponse<Page<ProductResponse>> {

        public ProductPageResponseBody() {
            super(true, "", null);
        }
    }

    // Orders

    public static class OrderResponseBody
            extends AppResponse<OrderResponse> {

        public OrderResponseBody() {
            super(true, "", null);
        }
    }

    public static class OrderListResponseBody
            extends AppResponse<List<OrderResponse>> {

        public OrderListResponseBody() {
            super(true, "", null);
        }
    }

    public static class OrderPageResponseBody
            extends AppResponse<Page<OrderResponse>> {

        public OrderPageResponseBody() {
            super(true, "", null);
        }
    }

    // Payments

    public static class PaymentResponseBody
            extends AppResponse<PaymentResponse> {

        public PaymentResponseBody() {
            super(true, "", null);
        }
    }

    public static class FinancialInstitutionsResponseBody
            extends AppResponse<List<WompiFinancialInstitutionsResponse>> {

        public FinancialInstitutionsResponseBody() {
            super(true, "", null);
        }
    }

    // Dashboard statistics

    public static class StatsResponseBody
            extends AppResponse<StatsResponse> {

        public StatsResponseBody() {
            super(true, "", null);
        }
    }

    // Banners

    public static class BannerResponseBody
            extends AppResponse<BannerResponse> {

        public BannerResponseBody() {
            super(true, "", null);
        }
    }

    public static class BannerListResponseBody
            extends AppResponse<List<BannerResponse>> {

        public BannerListResponseBody() {
            super(true, "", null);
        }
    }

    public static class ItemBannerResponseBody
            extends AppResponse<ItemBannerResponse> {

        public ItemBannerResponseBody() {
            super(true, "", null);
        }
    }

    public static class UploadedAssetResponseBody
            extends AppResponse<UploadedAssetResponse> {

        public UploadedAssetResponseBody() {
            super(true, "", null);
        }
    }
}
