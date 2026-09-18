package com.loopers.application.pay.point;

import java.util.Optional;

// 포인트 잔액 조회 쿼리
public interface PointQueryDao {
    Optional<Long> findBalance(long userId);
}
