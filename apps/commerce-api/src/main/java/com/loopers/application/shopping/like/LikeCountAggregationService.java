package com.loopers.application.shopping.like;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeCountAggregationService implements LikeCountAggregationUseCase {
    private final LikeCountAggregationDao aggregationDao;

    @Override
    @Transactional
    public void execute() {
        aggregationDao.resetAllCounts();
        aggregationDao.aggregateAllCounts();
    }
}
