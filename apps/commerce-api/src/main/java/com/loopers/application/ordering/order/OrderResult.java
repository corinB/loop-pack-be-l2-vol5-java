package com.loopers.application.ordering.order;

import com.loopers.domain.ordering.order.Order;
import com.loopers.domain.ordering.order.OrderStatus;
import java.time.Instant;
import java.util.List;

// 주문 결과
public record OrderResult(long orderId, long userId, OrderStatus status, long totalAmount, Instant createdAt,
                          List<OrderItemResult> items) {
    // 도메인 객체를 결과로 변환
    public static OrderResult from(Order order) {
        List<OrderItemResult> items = order.getItems().stream().map(OrderItemResult::from).toList();
        return new OrderResult(order.getId(), order.getUserId(), order.getStatus(), order.getTotalAmount(),
            order.getCreatedAt(), items);
    }
}
