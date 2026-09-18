package com.loopers.infrastructure.mall.brand;

import com.loopers.application.common.PageCriteria;
import com.loopers.application.common.PageResult;
import com.loopers.application.mall.brand.BrandDetail;
import com.loopers.application.mall.brand.BrandQueryDao;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JdbcBrandQueryDao implements BrandQueryDao {
    private final JdbcClient jdbcClient;

    @Override
    @Transactional(readOnly = true)
    public Optional<BrandDetail> findById(long brandId) {
        return jdbcClient.sql("SELECT id, name, description FROM brands WHERE id = :brandId AND deleted = false")
            .param("brandId", brandId)
            .query((rs, rowNum) -> new BrandDetail(rs.getLong("id"), rs.getString("name"), rs.getString("description")))
            .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BrandDetail> findAll(PageCriteria criteria) {
        long total = jdbcClient.sql("SELECT COUNT(*) FROM brands WHERE deleted = false").query(Long.class).single();
        List<BrandDetail> items = jdbcClient.sql("""
                SELECT id, name, description FROM brands
                WHERE deleted = false ORDER BY created_at DESC, id DESC LIMIT :size OFFSET :offset
                """)
            .param("size", criteria.size())
            .param("offset", criteria.offset())
            .query((rs, rowNum) -> new BrandDetail(rs.getLong("id"), rs.getString("name"), rs.getString("description")))
            .list();
        return PageResult.of(items, criteria.page(), criteria.size(), total);
    }
}
