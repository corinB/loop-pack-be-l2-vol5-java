package com.loopers.infrastructure.ordering.order;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 주문 Spring Data JPA 리포지토리
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

    // 비관적 쓰기 잠금으로 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderJpaEntity o where o.id = :id")
    Optional<OrderJpaEntity> findByIdForUpdate(@Param("id") long id);
}
