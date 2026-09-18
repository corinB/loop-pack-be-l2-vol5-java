package com.loopers.domain.mall.brand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BrandTest {
    @DisplayName("이름의 앞뒤 공백을 제거하고 브랜드를 생성한다")
    @Test
    void createsBrand_withTrimmedName() {
        Brand brand = Brand.create("  LOOP  ", null);

        assertThat(brand.getName()).isEqualTo("LOOP");
        assertThat(brand.getDescription()).isNull();
    }

    @DisplayName("잘못된 수정은 기존 상태를 유지한다")
    @Test
    void keepsState_whenUpdateFails() {
        Brand brand = Brand.create("기존", "설명");

        assertThatThrownBy(() -> brand.update(" ", "변경"))
            .isInstanceOf(DomainException.class)
            .extracting("errorCode")
            .isEqualTo(DomainErrorCode.INVALID_NAME);
        assertThat(brand.getName()).isEqualTo("기존");
        assertThat(brand.getDescription()).isEqualTo("설명");
    }

    @DisplayName("삭제된 브랜드는 수정할 수 없다")
    @Test
    void rejectsUpdate_whenDeleted() {
        Brand brand = Brand.restore(1L, "브랜드", null, true, Instant.now());

        assertThatThrownBy(() -> brand.update("변경", null))
            .isInstanceOf(DomainException.class)
            .extracting("errorCode")
            .isEqualTo(DomainErrorCode.DELETED_BRAND);
    }
}
