package com.loopers.application.mall.product;

// 상품 상세 조회 결과
public record ProductDetail(long productId, String name, long price, BrandSummary brand, long likeCount,
                            String description, int stock) {}
