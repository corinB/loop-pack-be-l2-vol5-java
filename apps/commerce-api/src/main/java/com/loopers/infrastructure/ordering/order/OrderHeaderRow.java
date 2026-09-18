package com.loopers.infrastructure.ordering.order;

import com.loopers.application.ordering.order.AdminOrderView;
import com.loopers.application.ordering.order.OrderItemView;
import com.loopers.application.ordering.order.OrderView;
import com.loopers.domain.ordering.order.OrderStatus;
import com.loopers.domain.pay.orderbill.OrderBillStatus;
import java.time.Instant;
import java.util.List;

// 주문 헤더 조회 결과 로우
record OrderHeaderRow(long orderId, long userId, OrderStatus status, long totalAmount, Long paymentAmount,
                      OrderBillStatus paymentStatus, Instant createdAt) {
    // 일반 사용자 응답 뷰로 변환
    OrderView toView(List<OrderItemView> items) {
        return new OrderView(orderId, status, totalAmount, paymentAmount, paymentStatus, createdAt, items);
    }

    // 관리자 응답 뷰로 변환
    AdminOrderView toAdminView(List<OrderItemView> items) {
        return new AdminOrderView(orderId, userId, status, totalAmount, paymentAmount, paymentStatus, createdAt,
            items);
    }
}
