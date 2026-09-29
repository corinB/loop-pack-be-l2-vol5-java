package com.loopers.infrastructure.persistence.shopping.jpa;

import com.loopers.infrastructure.persistence.shopping.entity.LikeJpaEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 좋아요 Spring Data JPA 저장소
public interface LikeJpaRepository extends JpaRepository<LikeJpaEntity, Long> {
    // 이미 있는 (userId, productId)는 무시하고 삽입
    @Modifying
    @Query("""
        insert into LikeJpaEntity (userId, productId, createdAt)
        values (:userId, :productId, :createdAt)
        on conflict do nothing
        """)
    int insertIfAbsent(@Param("userId") long userId, @Param("productId") long productId,
                       @Param("createdAt") Instant createdAt);

    @Modifying
    @Query("delete from LikeJpaEntity l where l.userId = :userId and l.productId = :productId")
    int deleteByUserIdAndProductId(@Param("userId") long userId, @Param("productId") long productId);
}
