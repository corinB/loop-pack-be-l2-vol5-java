package com.loopers.infrastructure.example;

import com.loopers.domain.example.ExampleModel;
import org.springframework.data.jpa.repository.JpaRepository;

// 예시 Spring Data JPA 레포지토리 (레거시 참고용)
public interface ExampleJpaRepository extends JpaRepository<ExampleModel, Long> {}
