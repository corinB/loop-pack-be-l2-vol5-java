package com.loopers.application.ordering.order;

import com.loopers.domain.ordering.order.Order;
import com.loopers.domain.ordering.order.OrderStatus;
import java.time.Instant;
import java.util.List;

public record OrderResult(long orderId, long userId, OrderStatus status, long totalAmount, Instant createdAt,
                          List<OrderItemResult> items) {
    public static OrderResult from(Order order) {
        List<OrderItemResult> items = order.getItems().stream().map(OrderItemResult::from).toList();
        return new OrderResult(order.getId(), order.getUserId(), order.getStatus(), order.getTotalAmount(),
            order.getCreatedAt(), items);
    }
}
