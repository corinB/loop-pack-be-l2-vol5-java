package com.loopers.application.mall.product;

// 상품 목록용 요약 정보
public record ProductSummary(long productId, String name, long price, BrandSummary brand, long likeCount) {}
