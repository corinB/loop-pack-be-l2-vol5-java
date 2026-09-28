package com.loopers.application.mall.usecase;

import com.loopers.application.mall.command.ProductCommand;
import com.loopers.application.mall.result.ProductResult;

// 상품 재고 설정 유스케이스
public interface SetProductStockUseCase {
    ProductResult execute(ProductCommand.SetStock command);
}
