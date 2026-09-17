package com.loopers.application.support.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApplicationExceptionTest {

    @DisplayName("별도 메시지가 없으면 애플리케이션 오류 코드의 메시지를 사용한다")
    @Test
    void usesErrorCodeMessage_whenMessageIsNotProvided() {
        ApplicationException exception = new ApplicationException(ApplicationErrorCode.USER_NOT_FOUND);

        assertThat(exception.getErrorCode()).isEqualTo(ApplicationErrorCode.USER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ApplicationErrorCode.USER_NOT_FOUND.getMessage());
    }

    @DisplayName("별도 메시지가 있으면 해당 메시지를 사용한다")
    @Test
    void usesCustomMessage_whenMessageIsProvided() {
        ApplicationException exception = new ApplicationException(ApplicationErrorCode.USER_NOT_FOUND, "custom message");

        assertThat(exception.getMessage()).isEqualTo("custom message");
    }
}
