package com.loopers.application.example;

import com.loopers.domain.example.ExampleModel;

// 예시 조회 결과 (레거시 참고용)
public record ExampleInfo(Long id, String name, String description) {
    public static ExampleInfo from(ExampleModel model) {
        return new ExampleInfo(
            model.getId(),
            model.getName(),
            model.getDescription()
        );
    }
}
