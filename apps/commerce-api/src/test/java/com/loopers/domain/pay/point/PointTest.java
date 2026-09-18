package com.loopers.domain.pay.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.domain.shared.Money;
import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PointTest {

    @DisplayName("포인트 생성")
    @Nested
    class Create {
        @DisplayName("사용자별 초기 잔액 0의 포인트를 생성한다")
        @Test
        void createsZeroBalancePoint() {
            Point point = Point.zero(1L);

            assertThat(point.getUserId()).isEqualTo(1L);
            assertThat(point.getBalance()).isZero();
        }

        @DisplayName("저장된 잔액으로 포인트를 복원한다")
        @Test
        void restoresPoint_withStoredBalance() {
            Point point = Point.restore(1L, 1_000L);

            assertThat(point.getBalance()).isEqualTo(1_000L);
        }

        @DisplayName("0 이하 사용자 ID로 포인트를 만들 수 없다")
        @ParameterizedTest
        @ValueSource(longs = {0L, -1L})
        void rejectsCreate_whenUserIdIsNotPositive(long userId) {
            assertThatThrownBy(() -> Point.zero(userId)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Point.restore(userId, 0L)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @DisplayName("포인트 충전")
    @Nested
    class Charge {
        @DisplayName("충전액만큼 잔액을 더한다")
        @Test
        void increasesBalance_byChargeAmount() {
            Point point = Point.zero(1L);

            point.charge(Money.positive(1_000L));

            assertThat(point.getBalance()).isEqualTo(1_000L);
        }

        @DisplayName("충전 후 잔액이 범위를 초과하면 거절하고 기존 잔액을 유지한다")
        @Test
        void rejectsOverflow_andKeepsOriginalBalance() {
            Point point = Point.restore(1L, Long.MAX_VALUE);

            assertThatThrownBy(() -> point.charge(Money.positive(1L)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(DomainErrorCode.CALCULATION_OVERFLOW);
            assertThat(point.getBalance()).isEqualTo(Long.MAX_VALUE);
        }
    }
}
