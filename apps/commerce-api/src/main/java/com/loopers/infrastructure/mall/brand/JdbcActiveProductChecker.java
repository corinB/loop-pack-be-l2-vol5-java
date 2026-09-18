package com.loopers.infrastructure.mall.brand;

import com.loopers.application.mall.brand.ActiveProductChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcActiveProductChecker implements ActiveProductChecker {
    private final JdbcClient jdbcClient;

    @Override
    public boolean existsByBrandId(long brandId) {
        return Boolean.TRUE.equals(jdbcClient.sql("SELECT EXISTS(SELECT 1 FROM products WHERE brand_id = :brandId AND deleted = false)")
            .param("brandId", brandId)
            .query(Boolean.class)
            .single());
    }
}
