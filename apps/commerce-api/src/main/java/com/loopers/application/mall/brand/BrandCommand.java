package com.loopers.application.mall.brand;

public final class BrandCommand {
    private BrandCommand() {}

    public record Create(String name, String description) {}

    public record Update(long brandId, String name, String description) {}

    public record Delete(long brandId) {}
}
