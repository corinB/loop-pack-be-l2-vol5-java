package com.loopers.infrastructure.pay.orderbill;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderBillJpaRepository extends JpaRepository<OrderBillJpaEntity, Long> {
    Optional<OrderBillJpaEntity> findByOrderId(long orderId);
}
