package com.loopers.infrastructure.pay.orderbill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.domain.pay.orderbill.OrderBill;
import com.loopers.domain.pay.orderbill.OrderBillRepository;
import com.loopers.utils.DatabaseCleanUp;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class OrderBillRepositoryIntegrationTest {
    @Autowired
    private OrderBillRepository orderBillRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("성공한 결제 기록을 저장하고 영속성 컨텍스트를 비워도 조회할 수 있다")
    @Test
    @Transactional
    void savesAndFindsOrderBill_byOrderId() {
        orderBillRepository.save(OrderBill.paid(1L, 1L, 7_000L));
        entityManager.flush();
        entityManager.clear();

        OrderBill restored = orderBillRepository.findByOrderId(1L).orElseThrow();

        assertThat(restored.getUserId()).isEqualTo(1L);
        assertThat(restored.getAmount()).isEqualTo(7_000L);
    }

    @DisplayName("같은 주문에는 결제 기록을 하나만 저장한다")
    @Test
    void enforcesUniqueOrderId() {
        orderBillRepository.save(OrderBill.paid(1L, 1L, 7_000L));

        assertThatThrownBy(() -> orderBillRepository.save(OrderBill.paid(1L, 1L, 7_000L)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("없는 주문의 결제 기록은 빈 결과를 반환한다")
    @Test
    @Transactional
    void returnsEmpty_whenOrderBillDoesNotExist() {
        assertThat(orderBillRepository.findByOrderId(999L)).isEmpty();
    }
}
