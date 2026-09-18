package com.loopers.application.pay.point;

import java.util.Optional;

public interface PointQueryDao {
    Optional<Long> findBalance(long userId);
}
