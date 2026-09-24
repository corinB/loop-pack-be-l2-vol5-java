package com.loopers.infrastructure.pay.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

// 지갑 Spring Data JPA 레포지토리
public interface WalletJpaRepository extends JpaRepository<WalletJpaEntity, Long> {}
