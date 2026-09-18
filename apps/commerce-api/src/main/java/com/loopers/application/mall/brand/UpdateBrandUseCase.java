package com.loopers.application.mall.brand;

public interface UpdateBrandUseCase {
    BrandResult execute(BrandCommand.Update command);
}
