package com.loopers.infrastructure.mall.brand;

import com.loopers.domain.mall.brand.Brand;
import org.springframework.stereotype.Component;

@Component
public class BrandEntityMapper {
    public Brand toDomain(BrandJpaEntity entity) {
        return Brand.restore(entity.getId(), entity.getName(), entity.getDescription(), entity.isDeleted(), entity.getCreatedAt());
    }

    public BrandJpaEntity toNewEntity(Brand brand) {
        return new BrandJpaEntity(brand.getName(), brand.getDescription(), brand.isDeleted());
    }

    public void apply(Brand brand, BrandJpaEntity entity) {
        entity.apply(brand.getName(), brand.getDescription(), brand.isDeleted());
    }
}
