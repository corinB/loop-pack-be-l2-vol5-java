package com.loopers.infrastructure.query.shopping;

import com.loopers.application.shopping.query.UserQueryDao;
import com.loopers.application.shopping.query.UserView;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
// 사용자 조회를 처리하는 JDBC DAO
public class JdbcUserQueryDao implements UserQueryDao {
    private final JdbcClient jdbcClient;

    // ID로 사용자 조회
    @Override
    @Transactional(readOnly = true)
    public Optional<UserView> findById(long userId) {
        return jdbcClient.sql("SELECT id FROM users WHERE id = :userId")
            .param("userId", userId)
            .query((resultSet, rowNum) -> new UserView(resultSet.getLong("id")))
            .optional();
    }
}
