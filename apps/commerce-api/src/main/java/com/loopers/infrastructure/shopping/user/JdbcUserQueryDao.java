package com.loopers.infrastructure.shopping.user;

import com.loopers.application.shopping.user.UserQueryDao;
import com.loopers.application.shopping.user.UserQueryModel;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JdbcUserQueryDao implements UserQueryDao {
    private final JdbcClient jdbcClient;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserQueryModel> findById(long userId) {
        return jdbcClient.sql("SELECT id FROM users WHERE id = :userId")
            .param("userId", userId)
            .query((resultSet, rowNum) -> new UserQueryModel(resultSet.getLong("id")))
            .optional();
    }
}
