package com.loopers.application.example;

import com.loopers.domain.example.ExampleModel;
import com.loopers.domain.example.ExampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
// 예시 파사드 (레거시 참고용)
public class ExampleFacade {
    private final ExampleService exampleService;

    // 예시 조회 후 결과로 변환
    public ExampleInfo getExample(Long id) {
        ExampleModel example = exampleService.getExample(id);
        return ExampleInfo.from(example);
    }
}
