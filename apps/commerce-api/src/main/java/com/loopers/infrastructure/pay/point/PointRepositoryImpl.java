package com.loopers.infrastructure.pay.point;

import com.loopers.domain.pay.point.Point;
import com.loopers.domain.pay.point.PointRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointRepositoryImpl implements PointRepository {
    private final PointJpaRepository pointJpaRepository;
    private final PointEntityMapper mapper;

    @Override
    public Point save(Point point) {
        Optional<PointJpaEntity> existing = pointJpaRepository.findById(point.getUserId());
        PointJpaEntity entity;
        if (existing.isEmpty()) {
            entity = mapper.toNewEntity(point);
        } else {
            entity = existing.get();
            mapper.apply(point, entity);
        }
        return mapper.toDomain(pointJpaRepository.save(entity));
    }

    @Override
    public Optional<Point> findByUserId(long userId) {
        return pointJpaRepository.findById(userId).map(mapper::toDomain);
    }
}
