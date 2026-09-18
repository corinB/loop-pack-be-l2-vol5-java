package com.loopers.application.ordering.order;

public interface ConfirmOrderUseCase {
    ConfirmOrderResult execute(ConfirmOrderCommand command);
}
