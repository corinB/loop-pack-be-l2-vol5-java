package com.loopers.infrastructure.pay.orderbill;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// 주문 결제 Spring Data JPA 레포지토리
public interface OrderBillJpaRepository extends JpaRepository<OrderBillJpaEntity, Long> {
    Optional<OrderBillJpaEntity> findByOrderId(long orderId);
}
