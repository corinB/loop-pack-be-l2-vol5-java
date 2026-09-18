package com.loopers.infrastructure.ordering.order;

import org.springframework.data.jpa.repository.JpaRepository;

// 주문 Spring Data JPA 리포지토리
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {}
