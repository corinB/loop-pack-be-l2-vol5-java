package com.loopers.application.shopping.user;

import java.util.Optional;

public interface UserQueryDao {
    Optional<UserQueryModel> findById(long userId);
}
