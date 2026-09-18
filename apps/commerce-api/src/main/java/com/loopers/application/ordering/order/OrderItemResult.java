package com.loopers.application.ordering.order;

import com.loopers.domain.ordering.order.OrderItem;

public record OrderItemResult(long productId, String productName, long unitPrice, int quantity, long amount) {
    public static OrderItemResult from(OrderItem item) {
        return new OrderItemResult(item.getProductId(), item.getProductName(), item.getUnitPrice(),
            item.getQuantity(), item.getAmount());
    }
}
