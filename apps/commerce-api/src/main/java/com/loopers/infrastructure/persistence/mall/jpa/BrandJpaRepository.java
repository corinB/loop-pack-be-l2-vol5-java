package com.loopers.infrastructure.persistence.mall.jpa;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loopers.infrastructure.persistence.mall.entity.BrandJpaEntity;

// 브랜드 Spring Data JPA 레포지토리
public interface BrandJpaRepository extends JpaRepository<BrandJpaEntity, Long> {
    // 삭제 전용 조회: 브랜드와 연결 상품 전체를 비관적 쓰기 잠금으로 함께 읽는다. 상품 없는 브랜드도 포함하며,
    // 저장 순서를 예측 가능하게 하기 위해 상품 id 순으로 정렬한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BrandJpaEntity b left join fetch b.products p where b.id = :brandId order by p.id")
    Optional<BrandJpaEntity> findForDeletion(@Param("brandId") long brandId);

    // 비관적 쓰기 잠금으로 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BrandJpaEntity b where b.id = :id")
    Optional<BrandJpaEntity> findByIdForUpdate(@Param("id") long id);

    // 비관적 공유 잠금으로 조회
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select b from BrandJpaEntity b where b.id = :id")
    Optional<BrandJpaEntity> findByIdForShare(@Param("id") long id);
}
