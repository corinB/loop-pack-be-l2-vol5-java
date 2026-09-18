package com.loopers.infrastructure.ordering.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.domain.ordering.order.Order;
import com.loopers.domain.ordering.order.OrderItem;
import com.loopers.domain.ordering.order.OrderRepository;
import com.loopers.domain.ordering.order.OrderStatus;
import com.loopers.utils.DatabaseCleanUp;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class OrderRepositoryIntegrationTest {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("주문과 품목을 저장하고 영속성 컨텍스트를 비워도 스냅샷과 합계를 보존한다")
    @Test
    @Transactional
    void savesOrder_withItemsAndTotalAmount() {
        List<OrderItem> items = List.of(
            OrderItem.create(1L, "상품1", 1_000L, 2),
            OrderItem.create(2L, "상품2", 3_000L, 1)
        );
        Order order = orderRepository.save(Order.create(1L, items));
        entityManager.flush();
        entityManager.clear();

        Order restored = orderRepository.findById(order.getId()).orElseThrow();

        assertThat(restored.getUserId()).isEqualTo(1L);
        assertThat(restored.getStatus()).isEqualTo(OrderStatus.DRAFT);
        assertThat(restored.getTotalAmount()).isEqualTo(5_000L);
        assertThat(restored.getItems()).hasSize(2);
        assertThat(restored.getItems().get(0).getProductName()).isEqualTo("상품1");
        assertThat(restored.getItems().get(0).getAmount()).isEqualTo(2_000L);
        assertThat(restored.getItems().get(1).getAmount()).isEqualTo(3_000L);
    }

    @DisplayName("없는 주문은 빈 결과를 반환한다")
    @Test
    @Transactional
    void returnsEmpty_whenOrderDoesNotExist() {
        assertThat(orderRepository.findById(999L)).isEmpty();
    }
}
