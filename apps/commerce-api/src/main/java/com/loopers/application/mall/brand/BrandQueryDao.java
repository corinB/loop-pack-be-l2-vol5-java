package com.loopers.application.mall.brand;

import com.loopers.application.common.PageCriteria;
import com.loopers.application.common.PageResult;
import java.util.Optional;

public interface BrandQueryDao {
    Optional<BrandDetail> findById(long brandId);

    PageResult<BrandDetail> findAll(PageCriteria criteria);
}
