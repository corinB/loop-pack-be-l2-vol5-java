package com.loopers.domain.mall.product;

import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);

    Optional<Product> findById(long productId);
}
