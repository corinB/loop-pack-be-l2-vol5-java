package com.loopers.application.pay.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.loopers.domain.pay.point.Point;
import com.loopers.domain.pay.point.PointBill;
import com.loopers.domain.pay.point.PointBillRepository;
import com.loopers.domain.pay.point.PointRepository;
import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class PointServiceTest {

    @DisplayName("포인트 충전")
    @Nested
    class Charge {
        @DisplayName("잔액을 늘려 저장한 뒤 CHARGE 기록을 순서대로 저장한다")
        @Test
        void chargesBalance_andSavesBillInOrder() {
            // arrange
            PointRepository pointRepository = mock(PointRepository.class);
            PointBillRepository pointBillRepository = mock(PointBillRepository.class);
            Point point = Point.zero(1L);
            given(pointRepository.findByUserId(1L)).willReturn(Optional.of(point));
            given(pointRepository.save(point)).willReturn(point);
            given(pointBillRepository.save(any(PointBill.class))).willAnswer(invocation -> invocation.getArgument(0));
            PointService service = new PointService(pointRepository, pointBillRepository);

            // act
            PointResult result = service.execute(new PointCommand.Charge(1L, 1_000L));

            // assert
            assertThat(result.balance()).isEqualTo(1_000L);
            InOrder order = inOrder(pointRepository, pointBillRepository);
            order.verify(pointRepository).findByUserId(1L);
            order.verify(pointRepository).save(point);
            order.verify(pointBillRepository).save(any(PointBill.class));
        }

        @DisplayName("충전 후 잔액이 범위를 초과하면 저장 없이 거절한다")
        @Test
        void rejectsOverflow_withoutSavingAnything() {
            // arrange
            PointRepository pointRepository = mock(PointRepository.class);
            PointBillRepository pointBillRepository = mock(PointBillRepository.class);
            Point point = Point.restore(1L, Long.MAX_VALUE);
            given(pointRepository.findByUserId(1L)).willReturn(Optional.of(point));
            PointService service = new PointService(pointRepository, pointBillRepository);

            // act & assert
            assertThatThrownBy(() -> service.execute(new PointCommand.Charge(1L, 1L)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(DomainErrorCode.CALCULATION_OVERFLOW);
            verify(pointRepository, never()).save(any());
            verify(pointBillRepository, never()).save(any());
        }

        @DisplayName("비양수 충전액은 저장 없이 거절한다")
        @Test
        void rejectsNonPositiveAmount_withoutSavingAnything() {
            // arrange
            PointRepository pointRepository = mock(PointRepository.class);
            PointBillRepository pointBillRepository = mock(PointBillRepository.class);
            given(pointRepository.findByUserId(1L)).willReturn(Optional.of(Point.zero(1L)));
            PointService service = new PointService(pointRepository, pointBillRepository);

            // act & assert
            assertThatThrownBy(() -> service.execute(new PointCommand.Charge(1L, 0L)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(DomainErrorCode.NON_POSITIVE_MONEY);
            verify(pointRepository, never()).save(any());
            verify(pointBillRepository, never()).save(any());
        }
    }
}
