package com.loopers.infrastructure.shopping.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.domain.shopping.user.User;
import com.loopers.domain.shopping.user.UserRepository;
import com.loopers.fixtures.UserFixture;
import com.loopers.utils.DatabaseCleanUp;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class UserRepositoryIntegrationTest {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("할당한 사용자 ID를 저장하고 영속성 컨텍스트를 비운 뒤 존재 여부를 조회한다")
    @Test
    @Transactional
    void savesAssignedId_andFindsExistenceAfterClear() {
        User saved = userRepository.save(UserFixture.firstUser());
        entityManager.flush();
        entityManager.clear();

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(userRepository.existsById(1L)).isTrue();
        assertThat(userRepository.existsById(2L)).isFalse();
    }
}
