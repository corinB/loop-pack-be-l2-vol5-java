package com.loopers.domain.pay.repository;

import com.loopers.domain.pay.model.PaymentAttempt;
import java.util.Optional;

public interface PaymentAttemptRepository {
    PaymentAttempt insert(PaymentAttempt attempt);
    PaymentAttempt save(PaymentAttempt attempt);
    Optional<PaymentAttempt> findByKey(String attemptKey);
    Optional<PaymentAttempt> findByOrderId(long orderId);
}
