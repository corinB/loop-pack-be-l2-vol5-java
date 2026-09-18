package com.loopers.domain.example;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
// 예시 도메인 서비스 (레거시 참고용)
public class ExampleService {

    private final ExampleRepository exampleRepository;

    // id로 예시 조회, 없으면 예외
    @Transactional(readOnly = true)
    public ExampleModel getExample(Long id) {
        return exampleRepository.find(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[id = " + id + "] 예시를 찾을 수 없습니다."));
    }
}
