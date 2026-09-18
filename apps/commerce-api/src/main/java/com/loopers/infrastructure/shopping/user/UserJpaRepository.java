package com.loopers.infrastructure.shopping.user;

import org.springframework.data.jpa.repository.JpaRepository;

// 사용자 Spring Data JPA 저장소
interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {}
