package com.loopers.infrastructure.pay.point;

import com.loopers.application.pay.point.PointQueryDao;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
// JdbcClient 기반 포인트 조회
public class JdbcPointQueryDao implements PointQueryDao {
    private final JdbcClient jdbcClient;

    // 사용자 포인트 잔액 조회
    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findBalance(long userId) {
        return jdbcClient.sql("SELECT balance FROM points WHERE user_id = :userId")
            .param("userId", userId)
            .query(Long.class)
            .optional();
    }
}
