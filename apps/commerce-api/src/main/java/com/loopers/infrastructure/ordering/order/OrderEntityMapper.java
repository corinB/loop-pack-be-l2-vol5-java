package com.loopers.infrastructure.ordering.order;

import com.loopers.domain.ordering.order.Order;
import com.loopers.domain.ordering.order.OrderItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderEntityMapper {
    public Order toDomain(OrderJpaEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
            .map(item -> OrderItem.restore(item.getProductId(), item.getProductName(), item.getUnitPrice(),
                item.getQuantity(), item.getAmount()))
            .toList();
        return Order.restore(entity.getId(), entity.getUserId(), entity.getStatus(), items, entity.getTotalAmount(),
            entity.getCreatedAt());
    }

    public OrderJpaEntity toNewEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity(order.getUserId(), order.getStatus(), order.getTotalAmount());
        for (OrderItem item : order.getItems()) {
            entity.addItem(new OrderItemJpaEntity(item.getProductId(), item.getProductName(), item.getUnitPrice(),
                item.getQuantity(), item.getAmount()));
        }
        return entity;
    }

    public void apply(Order order, OrderJpaEntity entity) {
        entity.apply(order.getStatus());
    }
}
