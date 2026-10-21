package com.loopers.domain.pg.repository;

import com.loopers.domain.pg.model.PgTransaction;
import java.util.Optional;

public interface PgTransactionRepository {
    void insert(PgTransaction transaction);
    Optional<PgTransaction> findByKey(String transactionKey);
}
