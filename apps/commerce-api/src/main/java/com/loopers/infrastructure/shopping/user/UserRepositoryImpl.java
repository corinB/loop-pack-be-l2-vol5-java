package com.loopers.infrastructure.shopping.user;

import com.loopers.domain.shopping.user.User;
import com.loopers.domain.shopping.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
// UserRepository JPA 구현체
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;
    private final UserEntityMapper userEntityMapper;

    // 사용자 저장
    @Override
    public User save(User user) {
        UserJpaEntity savedEntity = userJpaRepository.save(userEntityMapper.toEntity(user));
        return userEntityMapper.toDomain(savedEntity);
    }
}
