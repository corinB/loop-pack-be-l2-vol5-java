package com.loopers.infrastructure.shopping.user;

import com.loopers.domain.shopping.user.User;
import org.springframework.stereotype.Component;

@Component
public class UserEntityMapper {

    public UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(user.getId());
    }

    public User toDomain(UserJpaEntity entity) {
        return User.restore(entity.getId());
    }
}
