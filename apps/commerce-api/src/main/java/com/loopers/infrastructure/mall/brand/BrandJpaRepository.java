package com.loopers.infrastructure.mall.brand;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 브랜드 Spring Data JPA 레포지토리
public interface BrandJpaRepository extends JpaRepository<BrandJpaEntity, Long> {
    // 삭제 전용 조회: 브랜드와 연결 상품 전체를 함께 읽는다. 상품 없는 브랜드도 포함한다.
    @Query("select b from BrandJpaEntity b left join fetch b.products where b.id = :brandId")
    Optional<BrandJpaEntity> findForDeletion(@Param("brandId") long brandId);
}
