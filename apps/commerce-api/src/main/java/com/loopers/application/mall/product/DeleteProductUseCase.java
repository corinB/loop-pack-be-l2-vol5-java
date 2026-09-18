package com.loopers.application.mall.product;

public interface DeleteProductUseCase {
    void execute(ProductCommand.Delete command);
}
