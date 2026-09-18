package com.loopers.infrastructure.mall.product;

import org.springframework.data.jpa.repository.JpaRepository;

// 상품 Spring Data JPA 레포지토리
public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, Long> {}
