package com.loopers.domain.ordering.repository;

import com.loopers.domain.ordering.model.OrderRecord;
import java.util.Optional;

// 주문 기록 저장소
public interface OrderRecordRepository {
    OrderRecord save(OrderRecord orderRecord);

    Optional<OrderRecord> findByOrderId(long orderId);
}
