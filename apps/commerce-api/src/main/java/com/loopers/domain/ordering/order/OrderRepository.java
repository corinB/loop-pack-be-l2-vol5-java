package com.loopers.domain.ordering.order;

import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(long orderId);
}
