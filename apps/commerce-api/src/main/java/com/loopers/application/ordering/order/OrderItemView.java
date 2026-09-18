package com.loopers.application.ordering.order;

public record OrderItemView(long productId, String productName, long unitPrice, int quantity, long amount) {
    public static OrderItemView from(OrderItemResult result) {
        return new OrderItemView(result.productId(), result.productName(), result.unitPrice(), result.quantity(),
            result.amount());
    }
}
