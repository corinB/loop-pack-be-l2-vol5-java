package com.loopers.infrastructure.mall.brand;

import com.loopers.domain.mall.brand.Brand;
import com.loopers.domain.mall.brand.BrandRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BrandRepositoryImpl implements BrandRepository {
    private final BrandJpaRepository brandJpaRepository;
    private final BrandEntityMapper mapper;

    @Override
    public Brand save(Brand brand) {
        BrandJpaEntity entity;
        if (brand.getId() == null) {
            entity = mapper.toNewEntity(brand);
        } else {
            entity = brandJpaRepository.findById(brand.getId()).orElseThrow();
            mapper.apply(brand, entity);
        }
        return mapper.toDomain(brandJpaRepository.save(entity));
    }

    @Override
    public Optional<Brand> findById(long brandId) {
        return brandJpaRepository.findById(brandId).map(mapper::toDomain);
    }
}
