package com.loopers.application.mall.product;

public interface CreateProductUseCase {
    ProductResult execute(ProductCommand.Create command);
}
