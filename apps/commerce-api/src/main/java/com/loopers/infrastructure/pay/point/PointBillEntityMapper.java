package com.loopers.infrastructure.pay.point;

import com.loopers.domain.pay.point.PointBill;
import org.springframework.stereotype.Component;

@Component
public class PointBillEntityMapper {
    public PointBill toDomain(PointBillJpaEntity entity) {
        return PointBill.restore(entity.getId(), entity.getUserId(), entity.getType(), entity.getAmount(),
            entity.getCreatedAt());
    }

    public PointBillJpaEntity toNewEntity(PointBill pointBill) {
        return new PointBillJpaEntity(pointBill.getUserId(), pointBill.getType(), pointBill.getAmount());
    }
}
