package com.loopers.infrastructure.pay.orderbill;

import com.loopers.domain.pay.orderbill.OrderBill;
import org.springframework.stereotype.Component;

@Component
public class OrderBillEntityMapper {
    public OrderBill toDomain(OrderBillJpaEntity entity) {
        return OrderBill.restore(entity.getId(), entity.getOrderId(), entity.getUserId(), entity.getAmount(),
            entity.getStatus(), entity.getCreatedAt());
    }

    public OrderBillJpaEntity toNewEntity(OrderBill orderBill) {
        return new OrderBillJpaEntity(orderBill.getOrderId(), orderBill.getUserId(), orderBill.getAmount(),
            orderBill.getStatus());
    }
}
