package com.loopers.infrastructure.pay.point;

import com.loopers.domain.pay.point.Point;
import org.springframework.stereotype.Component;

@Component
public class PointEntityMapper {
    public Point toDomain(PointJpaEntity entity) {
        return Point.restore(entity.getUserId(), entity.getBalance());
    }

    public PointJpaEntity toNewEntity(Point point) {
        return new PointJpaEntity(point.getUserId(), point.getBalance());
    }

    public void apply(Point point, PointJpaEntity entity) {
        entity.apply(point.getBalance());
    }
}
