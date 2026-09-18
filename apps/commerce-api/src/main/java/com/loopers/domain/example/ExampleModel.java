package com.loopers.domain.example;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "example")
// 예시 도메인 엔티티 (레거시 참고용)
public class ExampleModel extends BaseEntity {

    private String name;
    private String description;

    protected ExampleModel() {}

    // 이름/설명 검증 후 생성
    public ExampleModel(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.");
        }
        if (description == null || description.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "설명은 비어있을 수 없습니다.");
        }

        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    // 설명 검증 후 갱신
    public void update(String newDescription) {
        if (newDescription == null || newDescription.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "설명은 비어있을 수 없습니다.");
        }
        this.description = newDescription;
    }
}
