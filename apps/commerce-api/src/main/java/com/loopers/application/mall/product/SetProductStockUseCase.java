package com.loopers.application.mall.product;

// 상품 재고 설정 유스케이스
public interface SetProductStockUseCase {
    ProductResult execute(ProductCommand.SetStock command);
}
