package com.loopers.infrastructure.mall.brand;

import org.springframework.data.jpa.repository.JpaRepository;

// 브랜드 Spring Data JPA 레포지토리
public interface BrandJpaRepository extends JpaRepository<BrandJpaEntity, Long> {}
