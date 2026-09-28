package com.loopers.infrastructure.persistence.pay.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.infrastructure.persistence.pay.entity.OrderBillJpaEntity;

// 주문 결제 Spring Data JPA 레포지토리
public interface OrderBillJpaRepository extends JpaRepository<OrderBillJpaEntity, Long> {
    Optional<OrderBillJpaEntity> findByOrderId(long orderId);
}
