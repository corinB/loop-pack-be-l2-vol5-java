package com.loopers.infrastructure.pay.point;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.application.pay.point.PointQueryDao;
import com.loopers.domain.pay.point.Point;
import com.loopers.domain.pay.point.PointRepository;
import com.loopers.domain.shared.Money;
import com.loopers.utils.DatabaseCleanUp;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class JdbcPointQueryDaoIntegrationTest {
    @Autowired
    private PointQueryDao pointQueryDao;
    @Autowired
    private PointRepository pointRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("잔액 조회")
    @Nested
    class FindBalance {
        @DisplayName("JPA로 저장한 잔액을 같은 DB에서 JDBC로 읽는다")
        @Test
        void findsStoredBalance() {
            Point point = Point.zero(1L);
            point.charge(Money.positive(1_000L));
            pointRepository.save(point);
            entityManager.flush();
            entityManager.clear();

            var balance = pointQueryDao.findBalance(1L);

            assertThat(balance).contains(1_000L);
        }

        @DisplayName("없는 사용자의 잔액은 빈 결과를 반환한다")
        @Test
        void returnsEmpty_whenPointDoesNotExist() {
            var balance = pointQueryDao.findBalance(999L);

            assertThat(balance).isEmpty();
        }
    }
}
