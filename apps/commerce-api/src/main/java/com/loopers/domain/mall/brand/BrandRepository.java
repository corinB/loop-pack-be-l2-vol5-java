package com.loopers.domain.mall.brand;

import java.util.Optional;

// 브랜드 저장소 인터페이스
public interface BrandRepository {
    Brand save(Brand brand);

    Optional<Brand> findById(long brandId);
}
