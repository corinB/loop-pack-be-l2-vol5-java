package com.loopers.infrastructure.pay.point;

import com.loopers.domain.pay.point.PointBill;
import com.loopers.domain.pay.point.PointBillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointBillRepositoryImpl implements PointBillRepository {
    private final PointBillJpaRepository pointBillJpaRepository;
    private final PointBillEntityMapper mapper;

    @Override
    public PointBill save(PointBill pointBill) {
        return mapper.toDomain(pointBillJpaRepository.save(mapper.toNewEntity(pointBill)));
    }
}
