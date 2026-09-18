package com.loopers.domain.mall.brand;

import java.util.Optional;

public interface BrandRepository {
    Brand save(Brand brand);

    Optional<Brand> findById(long brandId);
}
