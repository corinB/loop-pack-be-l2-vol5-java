package com.loopers.infrastructure.persistence.ordering.repository;

import com.loopers.domain.ordering.model.OrderRecord;
import com.loopers.domain.ordering.repository.OrderRecordRepository;
import com.loopers.infrastructure.persistence.ordering.entity.OrderRecordEntityMapper;
import com.loopers.infrastructure.persistence.ordering.jpa.OrderRecordJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
// 주문 기록 저장소 구현체
public class OrderRecordRepositoryImpl implements OrderRecordRepository {
    private final OrderRecordJpaRepository orderRecordJpaRepository;
    private final OrderRecordEntityMapper mapper;

    @Override
    public OrderRecord save(OrderRecord orderRecord) {
        return mapper.toDomain(orderRecordJpaRepository.save(mapper.toNewEntity(orderRecord)));
    }

    @Override
    public Optional<OrderRecord> findByOrderId(long orderId) {
        return orderRecordJpaRepository.findByOrderId(orderId).map(mapper::toDomain);
    }
}
