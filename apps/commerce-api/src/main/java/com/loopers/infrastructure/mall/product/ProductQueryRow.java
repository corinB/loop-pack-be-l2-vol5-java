package com.loopers.infrastructure.mall.product;

import com.loopers.application.mall.product.AdminProduct;
import com.loopers.application.mall.product.BrandSummary;
import com.loopers.application.mall.product.ProductDetail;
import com.loopers.application.mall.product.ProductSummary;
import java.time.Instant;

// QueryDSL 상품 조회 결과 행
public record ProductQueryRow(long productId, String name, long price, long brandId, String brandName,
                              long likeCount, String description, int stock, Instant createdAt) {
    ProductSummary toSummary() {
        return new ProductSummary(productId, name, price, brand(), likeCount);
    }

    ProductDetail toDetail() {
        return new ProductDetail(productId, name, price, brand(), likeCount, description, stock);
    }

    AdminProduct toAdminProduct() {
        return new AdminProduct(productId, name, price, brand(), likeCount, description, stock, createdAt);
    }

    private BrandSummary brand() {
        return new BrandSummary(brandId, brandName);
    }
}
