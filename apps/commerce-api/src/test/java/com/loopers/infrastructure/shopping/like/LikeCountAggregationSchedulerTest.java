package com.loopers.infrastructure.shopping.like;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.loopers.application.shopping.like.LikeCountAggregationUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LikeCountAggregationSchedulerTest {
    @DisplayName("집계 Service에 실행을 위임한다")
    @Test
    void delegatesAggregation() {
        LikeCountAggregationUseCase useCase = mock(LikeCountAggregationUseCase.class);
        LikeCountAggregationScheduler scheduler = new LikeCountAggregationScheduler(useCase);

        scheduler.aggregate();

        verify(useCase).execute();
    }

    @DisplayName("집계 실패를 전파하지 않아 다음 주기 실행을 유지한다")
    @Test
    void keepsSchedule_whenAggregationFails() {
        LikeCountAggregationUseCase useCase = mock(LikeCountAggregationUseCase.class);
        doThrow(new IllegalStateException("실패")).when(useCase).execute();
        LikeCountAggregationScheduler scheduler = new LikeCountAggregationScheduler(useCase);

        scheduler.aggregate();

        verify(useCase).execute();
    }
}
