package com.loopers.domain.support.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DomainExceptionTest {

    @DisplayName("별도 메시지가 없으면 도메인 오류 코드의 메시지를 사용한다")
    @Test
    void usesErrorCodeMessage_whenMessageIsNotProvided() {
        DomainException exception = new DomainException(DomainErrorCode.INVALID_USER_ID);

        assertThat(exception.getErrorCode()).isEqualTo(DomainErrorCode.INVALID_USER_ID);
        assertThat(exception.getMessage()).isEqualTo(DomainErrorCode.INVALID_USER_ID.getMessage());
    }

    @DisplayName("별도 메시지가 있으면 해당 메시지를 사용한다")
    @Test
    void usesCustomMessage_whenMessageIsProvided() {
        DomainException exception = new DomainException(DomainErrorCode.INVALID_USER_ID, "custom message");

        assertThat(exception.getMessage()).isEqualTo("custom message");
    }
}
