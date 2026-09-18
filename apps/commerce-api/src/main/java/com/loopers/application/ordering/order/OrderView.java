package com.loopers.application.ordering.order;

import com.loopers.domain.ordering.order.OrderStatus;
import com.loopers.domain.pay.orderbill.OrderBillStatus;
import java.time.Instant;
import java.util.List;

public record OrderView(long orderId, OrderStatus status, long totalAmount, Long paymentAmount,
                        OrderBillStatus paymentStatus, Instant createdAt, List<OrderItemView> items) {
    public static OrderView from(OrderResult result) {
        List<OrderItemView> items = result.items().stream().map(OrderItemView::from).toList();
        return new OrderView(result.orderId(), result.status(), result.totalAmount(), null, null,
            result.createdAt(), items);
    }
}
