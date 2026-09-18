package com.loopers.application.mall.brand;

// 브랜드 수정 유스케이스
public interface UpdateBrandUseCase {
    BrandResult execute(BrandCommand.Update command);
}
