package com.loopers.interfaces.api.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class XUserIdArgumentResolverTest {
    private final XUserIdArgumentResolver resolver = new XUserIdArgumentResolver();

    @DisplayName("XUserId가 붙은 long과 Long 파라미터를 지원한다")
    @Test
    void supportsAnnotatedLongParameters() throws Exception {
        assertThat(resolver.supportsParameter(parameter("primitive", long.class))).isTrue();
        assertThat(resolver.supportsParameter(parameter("boxed", Long.class))).isTrue();
    }

    @DisplayName("XUserId가 없거나 숫자 타입이 아닌 파라미터는 지원하지 않는다")
    @Test
    void doesNotSupportUnannotatedOrNonLongParameters() throws Exception {
        assertThat(resolver.supportsParameter(parameter("unannotated", long.class))).isFalse();
        assertThat(resolver.supportsParameter(parameter("text", String.class))).isFalse();
    }

    @DisplayName("양의 정수 헤더를 사용자 ID로 변환한다")
    @Test
    void resolvesPositiveUserId() throws Exception {
        Object userId = resolve("X-USER-ID", "1");

        assertThat(userId).isEqualTo(1L);
    }

    @DisplayName("헤더 이름은 대소문자를 구분하지 않는다")
    @Test
    void resolvesLowercaseHeaderName() throws Exception {
        Object userId = resolve("x-user-id", "2");

        assertThat(userId).isEqualTo(2L);
    }

    @DisplayName("헤더가 누락되거나 비어 있으면 필수 입력 오류가 발생한다")
    @ParameterizedTest
    @MethodSource("missingHeaders")
    void rejectsMissingHeader(String value) throws Exception {
        assertBadRequest(value, "X-USER-ID는 필수입니다.");
    }

    @DisplayName("양의 long 형식이 아니면 입력 형식 오류가 발생한다")
    @ParameterizedTest
    @MethodSource("invalidHeaders")
    void rejectsInvalidHeader(String value) throws Exception {
        assertBadRequest(value, "X-USER-ID는 양의 정수여야 합니다.");
    }

    private static Stream<Arguments> missingHeaders() {
        return Stream.of(Arguments.of((String) null), Arguments.of(""));
    }

    private static Stream<Arguments> invalidHeaders() {
        return Stream.of(
            Arguments.of("abc"),
            Arguments.of("1.0"),
            Arguments.of(" 1"),
            Arguments.of("1 "),
            Arguments.of(" "),
            Arguments.of("0"),
            Arguments.of("-1"),
            Arguments.of("9223372036854775808")
        );
    }

    private void assertBadRequest(String value, String message) throws Exception {
        assertThatThrownBy(() -> resolve("X-USER-ID", value))
            .isInstanceOfSatisfying(CoreException.class, exception -> {
                assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                assertThat(exception.getMessage()).isEqualTo(message);
            });
    }

    private Object resolve(String headerName, String headerValue) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (headerValue != null) {
            request.addHeader(headerName, headerValue);
        }
        return resolver.resolveArgument(
            parameter("primitive", long.class),
            null,
            new ServletWebRequest(request),
            null
        );
    }

    private MethodParameter parameter(String methodName, Class<?> parameterType) throws Exception {
        Method method = Parameters.class.getDeclaredMethod(methodName, parameterType);
        return new MethodParameter(method, 0);
    }

    private static class Parameters {
        void primitive(@XUserId long userId) {}

        void boxed(@XUserId Long userId) {}

        void unannotated(long userId) {}

        void text(@XUserId String userId) {}
    }
}
