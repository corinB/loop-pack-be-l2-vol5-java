package com.loopers.domain.pay.point;

import java.util.Optional;

public interface PointRepository {
    Point save(Point point);

    Optional<Point> findByUserId(long userId);
}
