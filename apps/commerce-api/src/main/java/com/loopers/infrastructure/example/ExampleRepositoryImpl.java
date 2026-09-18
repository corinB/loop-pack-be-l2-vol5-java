package com.loopers.infrastructure.example;

import com.loopers.domain.example.ExampleModel;
import com.loopers.domain.example.ExampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
// 예시 저장소 구현체 (레거시 참고용)
public class ExampleRepositoryImpl implements ExampleRepository {
    private final ExampleJpaRepository exampleJpaRepository;

    // id로 예시 조회
    @Override
    public Optional<ExampleModel> find(Long id) {
        return exampleJpaRepository.findById(id);
    }
}
