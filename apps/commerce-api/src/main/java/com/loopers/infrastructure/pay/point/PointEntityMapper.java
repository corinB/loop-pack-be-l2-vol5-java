package com.loopers.infrastructure.pay.point;

import com.loopers.domain.pay.point.Point;
import org.springframework.stereotype.Component;

@Component
// 포인트 엔티티-도메인 변환기
public class PointEntityMapper {
    // 엔티티를 도메인으로 변환
    public Point toDomain(PointJpaEntity entity) {
        return Point.restore(entity.getUserId(), entity.getBalance());
    }

    // 도메인을 신규 엔티티로 변환
    public PointJpaEntity toNewEntity(Point point) {
        return new PointJpaEntity(point.getUserId(), point.getBalance());
    }

    // 기존 엔티티에 변경사항 반영
    public void apply(Point point, PointJpaEntity entity) {
        entity.apply(point.getBalance());
    }
}
