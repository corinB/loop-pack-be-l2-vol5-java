package com.loopers.infrastructure.ordering.order;

import com.loopers.domain.ordering.order.Order;
import com.loopers.domain.ordering.order.OrderItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
// 주문 엔티티와 도메인 모델 변환
public class OrderEntityMapper {
    // 엔티티를 도메인으로 변환
    public Order toDomain(OrderJpaEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
            .map(item -> OrderItem.restore(item.getProductId(), item.getProductName(), item.getUnitPrice(),
                item.getQuantity(), item.getAmount()))
            .toList();
        return Order.restore(entity.getId(), entity.getUserId(), entity.getStatus(), items, entity.getTotalAmount(),
            entity.getCreatedAt());
    }

    // 도메인을 신규 엔티티로 변환
    public OrderJpaEntity toNewEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity(order.getUserId(), order.getStatus(), order.getTotalAmount());
        for (OrderItem item : order.getItems()) {
            entity.addItem(new OrderItemJpaEntity(item.getProductId(), item.getProductName(), item.getUnitPrice(),
                item.getQuantity(), item.getAmount()));
        }
        return entity;
    }

    // 도메인 상태를 기존 엔티티에 반영
    public void apply(Order order, OrderJpaEntity entity) {
        entity.apply(order.getStatus());
    }
}
