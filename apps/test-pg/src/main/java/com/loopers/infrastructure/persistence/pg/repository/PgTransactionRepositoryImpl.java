package com.loopers.infrastructure.persistence.pg.repository;

import com.loopers.domain.pg.model.PgTransaction;
import com.loopers.domain.pg.repository.PgTransactionRepository;
import com.loopers.infrastructure.persistence.pg.entity.PgTransactionEntityMapper;
import com.loopers.infrastructure.persistence.pg.entity.PgTransactionJpaEntity;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PgTransactionRepositoryImpl implements PgTransactionRepository {
    private final EntityManager entityManager;
    private final PgTransactionEntityMapper mapper;

    @Override
    public void insert(PgTransaction transaction) {
        entityManager.persist(mapper.toNewEntity(transaction));
    }

    @Override
    public Optional<PgTransaction> findByKey(String transactionKey) {
        return Optional.ofNullable(entityManager.find(PgTransactionJpaEntity.class, transactionKey)).map(mapper::toDomain);
    }
}
