package com.loopers.application.mall.product;

public record ProductDetail(long productId, String name, long price, BrandSummary brand, long likeCount,
                            String description, int stock) {}
