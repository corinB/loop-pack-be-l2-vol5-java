package com.loopers.application.mall.product;

// 상품 생성 유스케이스
public interface CreateProductUseCase {
    ProductResult execute(ProductCommand.Create command);
}
