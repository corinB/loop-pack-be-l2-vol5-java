package com.loopers.infrastructure.pay.orderbill;

import com.loopers.domain.pay.orderbill.OrderBill;
import com.loopers.domain.pay.orderbill.OrderBillRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
// 주문 결제 저장소 구현체
public class OrderBillRepositoryImpl implements OrderBillRepository {
    private final OrderBillJpaRepository orderBillJpaRepository;
    private final OrderBillEntityMapper mapper;

    @Override
    public OrderBill save(OrderBill orderBill) {
        return mapper.toDomain(orderBillJpaRepository.save(mapper.toNewEntity(orderBill)));
    }

    @Override
    public Optional<OrderBill> findByOrderId(long orderId) {
        return orderBillJpaRepository.findByOrderId(orderId).map(mapper::toDomain);
    }
}
