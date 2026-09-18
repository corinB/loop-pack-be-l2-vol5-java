package com.loopers.domain.pay.orderbill;

import java.util.Optional;

public interface OrderBillRepository {
    OrderBill save(OrderBill orderBill);

    Optional<OrderBill> findByOrderId(long orderId);
}
