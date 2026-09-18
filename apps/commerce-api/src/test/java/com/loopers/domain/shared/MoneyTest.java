package com.loopers.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @DisplayName("금액 생성")
    @Nested
    class Create {
        @DisplayName("0과 long 최댓값을 금액으로 표현한다")
        @Test
        void createsMoney_atBoundaries() {
            assertThat(Money.zero().getValue()).isZero();
            assertThat(Money.of(Long.MAX_VALUE).getValue()).isEqualTo(Long.MAX_VALUE);
        }

        @DisplayName("음수 또는 양수가 아닌 가격을 거절한다")
        @Test
        void rejectsInvalidMoney() {
            assertThatThrownBy(() -> Money.of(-1L))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(DomainErrorCode.INVALID_MONEY);
            assertThatThrownBy(() -> Money.positive(0L))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(DomainErrorCode.NON_POSITIVE_MONEY);
        }
    }

    @DisplayName("금액 계산")
    @Nested
    class Calculate {
        @DisplayName("금액을 더하고 양수 수량을 곱한다")
        @Test
        void calculatesMoney() {
            assertThat(Money.of(10L).add(Money.of(20L)).getValue()).isEqualTo(30L);
            assertThat(Money.of(10L).multiply(3).getValue()).isEqualTo(30L);
        }

        @DisplayName("덧셈과 곱셈 범위 초과를 거절하고 원래 금액을 유지한다")
        @Test
        void rejectsOverflow_andKeepsOriginalValue() {
            Money money = Money.of(Long.MAX_VALUE);

            assertThatThrownBy(() -> money.add(Money.of(1L)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(DomainErrorCode.CALCULATION_OVERFLOW);
            assertThatThrownBy(() -> money.multiply(2))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(DomainErrorCode.CALCULATION_OVERFLOW);
            assertThat(money.getValue()).isEqualTo(Long.MAX_VALUE);
        }
    }
}
