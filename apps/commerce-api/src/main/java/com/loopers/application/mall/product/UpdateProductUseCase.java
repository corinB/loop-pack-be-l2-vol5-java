package com.loopers.application.mall.product;

public interface UpdateProductUseCase {
    ProductResult execute(ProductCommand.Update command);
}
