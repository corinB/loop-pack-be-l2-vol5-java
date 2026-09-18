package com.loopers.infrastructure.shopping.like;

import com.loopers.domain.shopping.like.Like;
import com.loopers.domain.shopping.like.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LikeRepositoryImpl implements LikeRepository {
    private final LikeJpaRepository likeJpaRepository;
    private final LikeEntityMapper mapper;

    @Override
    public Like save(Like like) {
        if (like.getId() != null) {
            return like;
        }
        return mapper.toDomain(likeJpaRepository.save(mapper.toNewEntity(like)));
    }
}
