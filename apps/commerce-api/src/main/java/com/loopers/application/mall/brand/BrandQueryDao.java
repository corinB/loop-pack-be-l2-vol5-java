package com.loopers.application.mall.brand;

import com.loopers.application.common.PageCriteria;
import com.loopers.application.common.PageResult;
import java.util.Optional;

// 브랜드 조회 전용 DAO
public interface BrandQueryDao {
    Optional<BrandDetail> findById(long brandId);

    PageResult<BrandDetail> findAll(PageCriteria criteria);
}
