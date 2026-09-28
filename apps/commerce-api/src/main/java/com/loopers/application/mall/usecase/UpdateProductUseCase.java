package com.loopers.application.mall.usecase;

import com.loopers.application.mall.command.ProductCommand;
import com.loopers.application.mall.result.ProductResult;

// 상품 수정 유스케이스
public interface UpdateProductUseCase {
    ProductResult execute(ProductCommand.Update command);
}
