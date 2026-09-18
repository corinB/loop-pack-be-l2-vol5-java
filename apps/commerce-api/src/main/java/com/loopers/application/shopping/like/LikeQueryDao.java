package com.loopers.application.shopping.like;

import com.loopers.application.common.PageCriteria;
import com.loopers.application.common.PageResult;

public interface LikeQueryDao {
    PageResult<LikeItem> findByUserId(long userId, PageCriteria criteria);
}
