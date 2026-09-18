package com.loopers.infrastructure.ordering.order;

import com.loopers.domain.ordering.order.Order;
import com.loopers.domain.ordering.order.OrderRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {
    private final OrderJpaRepository orderJpaRepository;
    private final OrderEntityMapper mapper;

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity;
        if (order.getId() == null) {
            entity = mapper.toNewEntity(order);
        } else {
            entity = orderJpaRepository.findById(order.getId()).orElseThrow();
            mapper.apply(order, entity);
        }
        return mapper.toDomain(orderJpaRepository.save(entity));
    }

    @Override
    public Optional<Order> findById(long orderId) {
        return orderJpaRepository.findById(orderId).map(mapper::toDomain);
    }
}
