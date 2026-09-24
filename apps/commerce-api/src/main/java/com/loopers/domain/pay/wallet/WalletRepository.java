package com.loopers.domain.pay.wallet;

import java.util.Optional;

// 지갑 저장소
public interface WalletRepository {
    Wallet save(Wallet wallet);

    Optional<Wallet> findByUserId(long userId);
}
