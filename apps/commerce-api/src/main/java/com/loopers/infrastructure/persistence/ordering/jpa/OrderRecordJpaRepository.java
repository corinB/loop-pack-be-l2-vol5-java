package com.loopers.infrastructure.persistence.ordering.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.infrastructure.persistence.ordering.entity.OrderRecordJpaEntity;

// 주문 결제 Spring Data JPA 레포지토리
public interface OrderRecordJpaRepository extends JpaRepository<OrderRecordJpaEntity, Long> {
    Optional<OrderRecordJpaEntity> findByOrderId(long orderId);
}
