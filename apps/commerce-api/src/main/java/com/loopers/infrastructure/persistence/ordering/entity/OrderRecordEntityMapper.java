package com.loopers.infrastructure.persistence.ordering.entity;

import com.loopers.domain.ordering.model.OrderRecord;
import org.springframework.stereotype.Component;

@Component
// 주문 결제 엔티티-도메인 변환기
public class OrderRecordEntityMapper {
    // 엔티티를 도메인으로 변환
    public OrderRecord toDomain(OrderRecordJpaEntity entity) {
        return OrderRecord.restore(entity.getId(), entity.getOrderId(), entity.getUserId(), entity.getAmount(),
            entity.getStatus(), entity.getCreatedAt());
    }

    // 도메인을 신규 엔티티로 변환
    public OrderRecordJpaEntity toNewEntity(OrderRecord orderRecord) {
        return new OrderRecordJpaEntity(orderRecord.getOrderId(), orderRecord.getUserId(), orderRecord.getAmount(),
            orderRecord.getStatus());
    }
}
