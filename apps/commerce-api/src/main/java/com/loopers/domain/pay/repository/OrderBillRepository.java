package com.loopers.domain.pay.repository;

import com.loopers.domain.pay.model.OrderBill;
import java.util.Optional;

// 주문 결제 기록 저장소
public interface OrderBillRepository {
    OrderBill save(OrderBill orderBill);

    Optional<OrderBill> findByOrderId(long orderId);
}
