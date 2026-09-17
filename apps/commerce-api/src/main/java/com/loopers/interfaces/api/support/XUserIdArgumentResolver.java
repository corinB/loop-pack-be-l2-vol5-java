package com.loopers.interfaces.api.support;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class XUserIdArgumentResolver implements HandlerMethodArgumentResolver {
    static final String HEADER_NAME = "X-USER-ID";
    private static final String REQUIRED_MESSAGE = "X-USER-ID는 필수입니다.";
    private static final String INVALID_MESSAGE = "X-USER-ID는 양의 정수여야 합니다.";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Class<?> parameterType = parameter.getParameterType();
        return parameter.hasParameterAnnotation(XUserId.class)
            && (parameterType == long.class || parameterType == Long.class);
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
        String headerValue = webRequest.getHeader(HEADER_NAME);
        if (headerValue == null || headerValue.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, REQUIRED_MESSAGE);
        }

        try {
            long userId = Long.parseLong(headerValue);
            if (userId <= 0) {
                throw invalidUserId();
            }
            return userId;
        } catch (NumberFormatException e) {
            throw invalidUserId();
        }
    }

    private CoreException invalidUserId() {
        return new CoreException(ErrorType.BAD_REQUEST, INVALID_MESSAGE);
    }
}
