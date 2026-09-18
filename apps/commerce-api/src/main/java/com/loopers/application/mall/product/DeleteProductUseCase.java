package com.loopers.application.mall.product;

// 상품 삭제 유스케이스
public interface DeleteProductUseCase {
    void execute(ProductCommand.Delete command);
}
