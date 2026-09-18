package com.loopers.interfaces.api.pay.point;

import com.loopers.application.pay.point.PointCommand;
import com.loopers.application.pay.point.PointResult;
import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;

public final class PointApiDto {
    private PointApiDto() {}

    public record ChargeRequest(Long amount) {
        public PointCommand.Charge toCommand(long userId) {
            if (amount == null) {
                throw new DomainException(DomainErrorCode.NON_POSITIVE_MONEY);
            }
            return new PointCommand.Charge(userId, amount);
        }
    }

    public record BalanceResponse(long balance) {
        public static BalanceResponse from(PointResult result) {
            return new BalanceResponse(result.balance());
        }

        public static BalanceResponse from(long balance) {
            return new BalanceResponse(balance);
        }
    }
}
