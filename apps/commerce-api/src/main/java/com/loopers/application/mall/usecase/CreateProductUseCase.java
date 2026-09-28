package com.loopers.application.mall.usecase;

import com.loopers.application.mall.command.ProductCommand;
import com.loopers.application.mall.result.ProductResult;

// 상품 생성 유스케이스
public interface CreateProductUseCase {
    ProductResult execute(ProductCommand.Create command);
}
