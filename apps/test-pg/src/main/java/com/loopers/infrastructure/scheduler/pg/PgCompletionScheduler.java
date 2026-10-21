package com.loopers.infrastructure.scheduler.pg;

import com.loopers.application.pg.usecase.CompleteDuePgPaymentsUseCase;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pg.processing.enabled", havingValue = "true", matchIfMissing = true)
public class PgCompletionScheduler {
    private final CompleteDuePgPaymentsUseCase complete;

    @Scheduled(fixedDelayString = "${pg.processing.interval-ms:100}", scheduler = "pgProcessingScheduler")
    public void completeDue() {
        complete.execute(Instant.now());
    }
}
