package com.loopers.interfaces.api.support;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
// MVC 커스텀 아규먼트 리졸버 등록 설정
class ApiWebConfig implements WebMvcConfigurer {
    private final XUserIdArgumentResolver xUserIdArgumentResolver;

    // XUserId 리졸버 등록
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(xUserIdArgumentResolver);
    }
}
