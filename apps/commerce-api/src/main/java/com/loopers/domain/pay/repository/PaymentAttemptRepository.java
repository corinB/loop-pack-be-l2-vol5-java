package com.loopers.domain.pay.repository;

import com.loopers.domain.pay.model.PaymentAttempt;
import java.util.Optional;

public interface PaymentAttemptRepository {
    PaymentAttempt insert(PaymentAttempt attempt);
    PaymentAttempt save(PaymentAttempt attempt);
    Optional<PaymentAttempt> findByKey(String attemptKey);
    /** 짧은 관찰 저장 TX에서 최신 결제 사실을 순서대로 반영한다. 외부 호출 중에는 사용하지 않는다. */
    Optional<PaymentAttempt> findByKeyForUpdate(String attemptKey);
    Optional<PaymentAttempt> findByOrderId(long orderId);
}
