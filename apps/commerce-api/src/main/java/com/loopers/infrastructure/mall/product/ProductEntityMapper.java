package com.loopers.infrastructure.mall.product;

import com.loopers.domain.mall.product.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductEntityMapper {
    public Product toDomain(ProductJpaEntity entity) {
        return Product.restore(entity.getId(), entity.getBrandId(), entity.getName(), entity.getDescription(),
            entity.getPrice(), entity.getStock(), entity.isDeleted(), entity.getCreatedAt());
    }

    public ProductJpaEntity toNewEntity(Product product) {
        return new ProductJpaEntity(product.getBrandId(), product.getName(), product.getDescription(),
            product.getPrice(), product.getStock(), product.isDeleted());
    }

    public void apply(Product product, ProductJpaEntity entity) {
        entity.apply(product.getBrandId(), product.getName(), product.getDescription(), product.getPrice(),
            product.getStock(), product.isDeleted());
    }
}
