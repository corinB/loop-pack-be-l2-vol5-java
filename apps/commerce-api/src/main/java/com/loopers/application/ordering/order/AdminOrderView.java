package com.loopers.application.ordering.order;

import com.loopers.domain.ordering.order.OrderStatus;
import com.loopers.domain.pay.orderbill.OrderBillStatus;
import java.time.Instant;
import java.util.List;

// 관리자용 주문 조회 뷰
public record AdminOrderView(long orderId, long userId, OrderStatus status, long totalAmount, Long paymentAmount,
                             OrderBillStatus paymentStatus, Instant createdAt, List<OrderItemView> items) {}
