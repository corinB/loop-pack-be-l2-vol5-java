package com.loopers.infrastructure.pay.point;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.domain.pay.point.Point;
import com.loopers.domain.pay.point.PointRepository;
import com.loopers.domain.shared.Money;
import com.loopers.utils.DatabaseCleanUp;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class PointRepositoryIntegrationTest {
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

    @DisplayName("사용자별 초기 포인트를 저장하고 충전한 뒤 영속성 컨텍스트를 비워도 잔액을 보존한다")
    @Test
    @Transactional
    void savesAndChargesPoint() {
        Point point = pointRepository.save(Point.zero(1L));
        point.charge(Money.positive(1_000L));
        pointRepository.save(point);
        entityManager.flush();
        entityManager.clear();

        Point restored = pointRepository.findByUserId(1L).orElseThrow();

        assertThat(restored.getUserId()).isEqualTo(1L);
        assertThat(restored.getBalance()).isEqualTo(1_000L);
    }

    @DisplayName("없는 사용자의 포인트는 빈 결과를 반환한다")
    @Test
    @Transactional
    void returnsEmpty_whenPointDoesNotExist() {
        assertThat(pointRepository.findByUserId(999L)).isEmpty();
    }
}
