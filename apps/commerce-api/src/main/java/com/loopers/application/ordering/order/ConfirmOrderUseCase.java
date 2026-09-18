package com.loopers.application.ordering.order;

// 주문 확정 유스케이스
public interface ConfirmOrderUseCase {
    ConfirmOrderResult execute(ConfirmOrderCommand command);
}
