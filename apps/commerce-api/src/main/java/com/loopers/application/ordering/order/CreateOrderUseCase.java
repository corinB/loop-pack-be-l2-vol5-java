package com.loopers.application.ordering.order;

// 주문 생성 유스케이스
public interface CreateOrderUseCase {
    OrderResult execute(OrderCommand.Create command);
}
