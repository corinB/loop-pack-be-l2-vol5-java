package com.loopers.application.mall.brand;

// 브랜드에 활성 상품 존재 여부 확인
public interface ActiveProductChecker {
    boolean existsByBrandId(long brandId);
}
