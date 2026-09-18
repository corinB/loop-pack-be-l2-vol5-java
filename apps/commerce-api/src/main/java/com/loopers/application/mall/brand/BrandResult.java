package com.loopers.application.mall.brand;

import com.loopers.domain.mall.brand.Brand;

public record BrandResult(long brandId, String name, String description) {
    public static BrandResult from(Brand brand) {
        return new BrandResult(brand.getId(), brand.getName(), brand.getDescription());
    }
}
