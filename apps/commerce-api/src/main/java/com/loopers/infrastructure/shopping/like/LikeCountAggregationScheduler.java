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
// 좋아요 수 주기적 집계 스케줄러
public class LikeCountAggregationScheduler {
    private final LikeCountAggregationUseCase aggregationUseCase;

    // 주기적으로 좋아요 수 집계 실행
    @Scheduled(initialDelay = 0, fixedDelay = 10_000)
    public void aggregate() {
        try {
            aggregationUseCase.execute();
        } catch (RuntimeException exception) {
            log.error("상품 좋아요 수 전체 집계에 실패했습니다. 다음 주기에 다시 시도합니다.", exception);
        }
    }
}
