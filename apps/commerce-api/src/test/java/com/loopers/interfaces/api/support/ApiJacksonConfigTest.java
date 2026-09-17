package com.loopers.interfaces.api.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;

@JsonTest
@Import(ApiJacksonConfig.class)
class ApiJacksonConfigTest {
    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("JSON 정수를 long 값으로 역직렬화한다")
    @Test
    void readsInteger() throws Exception {
        IntegerRequest request = objectMapper.readValue("{\"value\":0}", IntegerRequest.class);

        assertThat(request.value()).isZero();
    }

    @DisplayName("문자열·빈 문자열·소수는 정수로 보정하지 않는다")
    @ParameterizedTest
    @ValueSource(strings = {"\"1\"", "\"\"", "1.0"})
    void rejectsCoercedInteger(String value) {
        assertThatThrownBy(() -> objectMapper.readValue(json(value), IntegerRequest.class))
            .isInstanceOf(Exception.class);
    }

    @DisplayName("null은 primitive 정수의 기본값으로 보정하지 않는다")
    @Test
    void rejectsNullInteger() {
        assertThatThrownBy(() -> objectMapper.readValue(json("null"), IntegerRequest.class))
            .isInstanceOf(Exception.class);
    }

    @DisplayName("long 표현 범위를 넘는 정수는 거절한다")
    @Test
    void rejectsIntegerOverflow() {
        assertThatThrownBy(() -> objectMapper.readValue(
            json("9223372036854775808"),
            IntegerRequest.class
        )).isInstanceOf(Exception.class);
    }

    private String json(String value) {
        return "{\"value\":" + value + "}";
    }

    private record IntegerRequest(long value) {}
}
