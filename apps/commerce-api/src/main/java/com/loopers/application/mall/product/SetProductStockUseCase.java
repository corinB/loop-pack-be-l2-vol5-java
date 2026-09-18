package com.loopers.application.mall.product;

public interface SetProductStockUseCase {
    ProductResult execute(ProductCommand.SetStock command);
}
