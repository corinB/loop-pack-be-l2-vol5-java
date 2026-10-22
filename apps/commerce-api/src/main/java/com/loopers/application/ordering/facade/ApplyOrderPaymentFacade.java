package com.loopers.application.ordering.facade;

import com.loopers.application.ordering.result.ApplyOrderPaymentResult;
import com.loopers.application.ordering.service.ApplyOrderPaymentService;
import com.loopers.application.ordering.usecase.ApplyOrderPaymentUseCase;
import com.loopers.application.pay.service.PaymentRecoveryProgressService;
import com.loopers.domain.pay.model.PaymentAttempt;
import com.loopers.domain.pay.model.PaymentRecoveryTask;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyOrderPaymentFacade implements ApplyOrderPaymentUseCase {
    private final ApplyOrderPaymentService application;
    private final PaymentRecoveryProgressService progress;

    @Override
    public ApplyOrderPaymentResult execute(String attemptKey) {
        String key = PaymentAttempt.normalizeKey(attemptKey);
        PaymentRecoveryTask.Snapshot expected = progress.capture(key);
        Optional<ApplyOrderPaymentResult> applied;
        try {
            applied = application.apply(key);
        } catch (RuntimeException failure) {
            log.warn("주문 결제 업무 반영에 실패했습니다. attemptKey={}", key, failure);
            try {
                progress.markApplicationFailed(expected);
            } catch (RuntimeException markerFailure) {
                log.warn("업무 실패 표식을 저장하지 못했습니다. 현재 상태를 다시 확인합니다. attemptKey={}", key, markerFailure);
            }
            return application.currentResult(key, true);
        }
        return applied.orElseGet(() -> application.currentResult(key, false));
    }
}
