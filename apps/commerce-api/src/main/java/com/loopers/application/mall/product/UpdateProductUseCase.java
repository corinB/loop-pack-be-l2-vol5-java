package com.loopers.application.mall.product;

// 상품 수정 유스케이스
public interface UpdateProductUseCase {
    ProductResult execute(ProductCommand.Update command);
}
