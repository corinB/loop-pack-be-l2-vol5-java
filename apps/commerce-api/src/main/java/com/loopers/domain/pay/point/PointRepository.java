package com.loopers.domain.pay.point;

import java.util.Optional;

// 포인트 저장소
public interface PointRepository {
    Point save(Point point);

    Optional<Point> findByUserId(long userId);
}
