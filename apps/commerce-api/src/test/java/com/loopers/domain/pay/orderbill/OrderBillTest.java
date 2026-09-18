package com.loopers.domain.pay.orderbill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OrderBillTest {

    @DisplayName("결제 기록 생성")
    @Nested
    class Paid {
        @DisplayName("성공한 결제만 PAID 상태로 생성한다")
        @Test
        void createsPaidOrderBill() {
            OrderBill bill = OrderBill.paid(1L, 1L, 7_000L);

            assertThat(bill.getOrderId()).isEqualTo(1L);
            assertThat(bill.getUserId()).isEqualTo(1L);
            assertThat(bill.getAmount()).isEqualTo(7_000L);
            assertThat(bill.getStatus()).isEqualTo(OrderBillStatus.PAID);
        }

        @DisplayName("0 이하 결제액은 거절한다")
        @Test
        void rejectsPaid_whenAmountIsNotPositive() {
            assertThatThrownBy(() -> OrderBill.paid(1L, 1L, 0L)).isInstanceOf(RuntimeException.class);
        }
    }

    @DisplayName("결제 기록 복원")
    @Nested
    class Restore {
        @DisplayName("저장된 상태를 복원한다")
        @Test
        void restoresOrderBill() {
            Instant createdAt = Instant.now();

            OrderBill bill = OrderBill.restore(1L, 1L, 1L, 7_000L, OrderBillStatus.PAID, createdAt);

            assertThat(bill.getId()).isEqualTo(1L);
            assertThat(bill.getCreatedAt()).isEqualTo(createdAt);
        }

        @DisplayName("id가 없거나 생성 시각이 없으면 거절한다")
        @Test
        void rejectsRestore_whenMetadataIsMissing() {
            assertThatThrownBy(() -> OrderBill.restore(0L, 1L, 1L, 7_000L, OrderBillStatus.PAID, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> OrderBill.restore(1L, 1L, 1L, 7_000L, OrderBillStatus.PAID, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
