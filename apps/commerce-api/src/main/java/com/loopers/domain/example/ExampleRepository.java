package com.loopers.domain.example;

import java.util.Optional;

// 예시 저장소 (레거시 참고용)
public interface ExampleRepository {
    Optional<ExampleModel> find(Long id);
}
