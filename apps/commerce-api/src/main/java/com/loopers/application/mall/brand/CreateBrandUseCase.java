package com.loopers.application.mall.brand;

public interface CreateBrandUseCase {
    BrandResult execute(BrandCommand.Create command);
}
