package com.loopers.application.mall.product;

public record ProductSummary(long productId, String name, long price, BrandSummary brand, long likeCount) {}
