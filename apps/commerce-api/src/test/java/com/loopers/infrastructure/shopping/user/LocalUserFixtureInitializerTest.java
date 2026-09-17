package com.loopers.infrastructure.shopping.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.domain.shopping.user.User;
import com.loopers.domain.shopping.user.UserRepository;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocalUserFixtureInitializerTest {

    @DisplayName("local fixture 초기화를 반복해도 사용자 1과 2만 한 번씩 저장한다")
    @Test
    void initializesTwoUsersIdempotently() throws Exception {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        LocalUserFixtureInitializer initializer = new LocalUserFixtureInitializer(repository);

        initializer.run(null);
        initializer.run(null);

        assertThat(repository.users).containsOnlyKeys(1L, 2L);
        assertThat(repository.saveCount).isEqualTo(2);
    }

    private static class InMemoryUserRepository implements UserRepository {
        private final Map<Long, User> users = new HashMap<>();
        private int saveCount;

        @Override
        public User save(User user) {
            users.put(user.getId(), user);
            saveCount++;
            return user;
        }

        @Override
        public boolean existsById(long userId) {
            return users.containsKey(userId);
        }
    }
}
