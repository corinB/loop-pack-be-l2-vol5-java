package com.loopers.infrastructure.shopping.like;

import com.loopers.application.shopping.like.LikeCountAggregationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "scheduler.like-count.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class LikeCountAggregationScheduler {
    private final LikeCountAggregationUseCase aggregationUseCase;

    @Scheduled(initialDelay = 0, fixedDelay = 10_000)
    public void aggregate() {
        try {
            aggregationUseCase.execute();
        } catch (RuntimeException exception) {
            log.error("상품 좋아요 수 전체 집계에 실패했습니다. 다음 주기에 다시 시도합니다.", exception);
        }
    }
}
