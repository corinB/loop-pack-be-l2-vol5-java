package com.loopers.infrastructure.mall.brand;

import com.loopers.application.mall.brand.ActiveProductChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
// JDBC 기반 활성 상품 존재 확인
public class JdbcActiveProductChecker implements ActiveProductChecker {
    private final JdbcClient jdbcClient;

    // 브랜드에 활성 상품 존재 여부 조회
    @Override
    public boolean existsByBrandId(long brandId) {
        return Boolean.TRUE.equals(jdbcClient.sql("SELECT EXISTS(SELECT 1 FROM products WHERE brand_id = :brandId AND deleted = false)")
            .param("brandId", brandId)
            .query(Boolean.class)
            .single());
    }
}
