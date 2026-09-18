package com.loopers.application.shopping.like;

import com.loopers.application.common.PageCriteria;
import com.loopers.application.common.PageResult;

// 좋아요 목록 조회용 DAO
public interface LikeQueryDao {
    PageResult<LikeItem> findByUserId(long userId, PageCriteria criteria);
}
