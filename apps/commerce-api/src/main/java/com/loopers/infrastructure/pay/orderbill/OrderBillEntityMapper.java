package com.loopers.infrastructure.pay.orderbill;

import com.loopers.domain.pay.orderbill.OrderBill;
import org.springframework.stereotype.Component;

@Component
// 주문 결제 엔티티-도메인 변환기
public class OrderBillEntityMapper {
    // 엔티티를 도메인으로 변환
    public OrderBill toDomain(OrderBillJpaEntity entity) {
        return OrderBill.restore(entity.getId(), entity.getOrderId(), entity.getUserId(), entity.getAmount(),
            entity.getStatus(), entity.getCreatedAt());
    }

    // 도메인을 신규 엔티티로 변환
    public OrderBillJpaEntity toNewEntity(OrderBill orderBill) {
        return new OrderBillJpaEntity(orderBill.getOrderId(), orderBill.getUserId(), orderBill.getAmount(),
            orderBill.getStatus());
    }
}
