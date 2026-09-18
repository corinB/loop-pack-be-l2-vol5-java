package com.loopers.application.ordering.order;

public interface CreateOrderUseCase {
    OrderResult execute(OrderCommand.Create command);
}
