package com.loopers.application.mall.brand;

// 브랜드 생성 유스케이스
public interface CreateBrandUseCase {
    BrandResult execute(BrandCommand.Create command);
}
