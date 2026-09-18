package com.loopers.infrastructure.mall.product;

import com.loopers.domain.mall.product.Product;
import com.loopers.domain.mall.product.ProductRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {
    private final ProductJpaRepository productJpaRepository;
    private final ProductEntityMapper mapper;

    @Override
    public Product save(Product product) {
        ProductJpaEntity entity;
        if (product.getId() == null) {
            entity = mapper.toNewEntity(product);
        } else {
            entity = productJpaRepository.findById(product.getId()).orElseThrow();
            mapper.apply(product, entity);
        }
        return mapper.toDomain(productJpaRepository.save(entity));
    }

    @Override
    public Optional<Product> findById(long productId) {
        return productJpaRepository.findById(productId).map(mapper::toDomain);
    }
}
