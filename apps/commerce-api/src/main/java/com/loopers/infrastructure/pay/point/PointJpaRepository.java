package com.loopers.infrastructure.pay.point;

import org.springframework.data.jpa.repository.JpaRepository;

// 포인트 Spring Data JPA 레포지토리
public interface PointJpaRepository extends JpaRepository<PointJpaEntity, Long> {}
