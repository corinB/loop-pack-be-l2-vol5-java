package com.loopers.application.shopping.user;

import java.util.Optional;

// 사용자 조회용 DAO
public interface UserQueryDao {
    Optional<UserQueryModel> findById(long userId);
}
