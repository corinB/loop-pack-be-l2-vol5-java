package com.loopers.application.shopping.user;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.loopers.application.support.error.ApplicationErrorCode;
import com.loopers.application.support.error.ApplicationException;
import com.loopers.domain.shopping.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserValidatorTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserValidator userValidator = new UserValidator(userRepository);

    @DisplayName("사용자가 존재하면 검증을 통과한다")
    @Test
    void passes_whenUserExists() {
        given(userRepository.existsById(1L)).willReturn(true);

        assertThatCode(() -> userValidator.validate(1L)).doesNotThrowAnyException();
    }

    @DisplayName("사용자가 없으면 USER_NOT_FOUND 오류가 발생한다")
    @Test
    void throwsUserNotFound_whenUserDoesNotExist() {
        given(userRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> userValidator.validate(1L))
            .isInstanceOf(ApplicationException.class)
            .extracting("errorCode")
            .isEqualTo(ApplicationErrorCode.USER_NOT_FOUND);
    }
}
