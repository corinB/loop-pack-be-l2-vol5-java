package com.loopers.infrastructure.shopping.user;

import com.loopers.domain.shopping.user.User;
import com.loopers.domain.shopping.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;
    private final UserEntityMapper userEntityMapper;

    @Override
    public User save(User user) {
        UserJpaEntity savedEntity = userJpaRepository.save(userEntityMapper.toEntity(user));
        return userEntityMapper.toDomain(savedEntity);
    }

    @Override
    public boolean existsById(long userId) {
        return userJpaRepository.existsById(userId);
    }
}
