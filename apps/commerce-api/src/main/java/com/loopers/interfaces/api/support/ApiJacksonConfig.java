package com.loopers.interfaces.api.support;

import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ApiJacksonConfig {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer strictIntegerCustomizer() {
        return builder -> builder.postConfigurer(objectMapper -> {
            objectMapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
            var integerCoercion = objectMapper.coercionConfigFor(LogicalType.Integer);
            integerCoercion.setCoercion(CoercionInputShape.String, CoercionAction.Fail);
            integerCoercion.setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail);
            integerCoercion.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
        });
    }
}
