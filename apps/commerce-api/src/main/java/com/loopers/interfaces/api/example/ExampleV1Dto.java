package com.loopers.interfaces.api.example;

import com.loopers.application.example.ExampleInfo;

// 예시 API DTO 모음 (레거시 참고용)
public class ExampleV1Dto {
    // 예시 응답
    public record ExampleResponse(Long id, String name, String description) {
        public static ExampleResponse from(ExampleInfo info) {
            return new ExampleResponse(
                info.id(),
                info.name(),
                info.description()
            );
        }
    }
}
