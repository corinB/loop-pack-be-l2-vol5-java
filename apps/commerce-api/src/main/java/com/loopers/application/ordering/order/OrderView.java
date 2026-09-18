package com.loopers.application.ordering.order;

import com.loopers.domain.ordering.order.OrderStatus;
import com.loopers.domain.pay.orderbill.OrderBillStatus;
import java.time.Instant;
import java.util.List;

// 주문 조회 응답 뷰
public record OrderView(long orderId, OrderStatus status, long totalAmount, Long paymentAmount,
                        OrderBillStatus paymentStatus, Instant createdAt, List<OrderItemView> items) {
    // 결제 정보 없이 변환
    public static OrderView from(OrderResult result) {
        return of(result, null, null);
    }

    // 결제 정보를 포함해 변환
    public static OrderView of(OrderResult result, Long paymentAmount, OrderBillStatus paymentStatus) {
        List<OrderItemView> items = result.items().stream().map(OrderItemView::from).toList();
        return new OrderView(result.orderId(), result.status(), result.totalAmount(), paymentAmount, paymentStatus,
            result.createdAt(), items);
    }
}
