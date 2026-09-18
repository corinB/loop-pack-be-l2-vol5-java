package com.loopers.infrastructure.ordering.order;

import com.loopers.application.ordering.order.OrderItemView;

record OrderItemRow(long orderId, long productId, String productName, long unitPrice, int quantity, long amount) {
    OrderItemView toView() {
        return new OrderItemView(productId, productName, unitPrice, quantity, amount);
    }
}
