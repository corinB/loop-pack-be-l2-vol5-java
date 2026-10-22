package com.loopers.application.ordering.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.loopers.application.ordering.result.ApplyOrderPaymentResult;
import com.loopers.application.ordering.service.ApplyOrderPaymentService;
import com.loopers.application.pay.service.PaymentRecoveryProgressService;
import com.loopers.domain.pay.model.PaymentRecoveryTask;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ApplyOrderPaymentFacadeTest {
    private final ApplyOrderPaymentService application = mock(ApplyOrderPaymentService.class);
    private final PaymentRecoveryProgressService progress = mock(PaymentRecoveryProgressService.class);
    private final ApplyOrderPaymentFacade facade = new ApplyOrderPaymentFacade(application, progress);
    private final String key = UUID.randomUUID().toString();

    @ParameterizedTest
    @EnumSource(value = ApplyOrderPaymentResult.State.class, names = {"APPLIED", "APPLICATION_FAILED"})
    @DisplayName("수동 표식 저장만 실패해도 새 조회로 확인한 완료 또는 미완료 상태를 반환한다")
    void readsCurrentStateAfterMarkerFailure(ApplyOrderPaymentResult.State state) {
        arrangeMarkerFailure();
        ApplyOrderPaymentResult expected = new ApplyOrderPaymentResult(state, true);
        given(application.currentResult(key, true)).willReturn(expected);

        assertThat(facade.execute(key)).isEqualTo(expected);
    }

    @Test
    @DisplayName("표식 저장과 상태 조회가 모두 실패하면 조회 장애를 숨기지 않는다")
    void propagatesCurrentReadFailure() {
        arrangeMarkerFailure();
        RuntimeException readFailure = new IllegalStateException("상태 조회 장애");
        given(application.currentResult(key, true)).willThrow(readFailure);

        assertThatThrownBy(() -> facade.execute(key)).isSameAs(readFailure);
    }

    private void arrangeMarkerFailure() {
        PaymentRecoveryTask.Snapshot expected = PaymentRecoveryTask.create(key).snapshot();
        given(progress.capture(key)).willReturn(expected);
        given(application.apply(key)).willThrow(new IllegalStateException("업무 실패"));
        given(progress.markApplicationFailed(expected)).willThrow(new IllegalStateException("표식 저장 실패"));
    }
}
