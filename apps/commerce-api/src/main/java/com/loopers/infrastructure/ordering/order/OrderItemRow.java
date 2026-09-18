package com.loopers.infrastructure.ordering.order;

import com.loopers.application.ordering.order.OrderItemView;

// 주문 품목 조회 결과 로우
record OrderItemRow(long orderId, long productId, String productName, long unitPrice, int quantity, long amount) {
    // 응답 뷰로 변환
    OrderItemView toView() {
        return new OrderItemView(productId, productName, unitPrice, quantity, amount);
    }
}
