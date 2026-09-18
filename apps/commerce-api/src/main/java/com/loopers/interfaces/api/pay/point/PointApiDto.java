package com.loopers.interfaces.api.pay.point;

import com.loopers.application.pay.point.PointCommand;
import com.loopers.application.pay.point.PointResult;
import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;

// 포인트 API 요청/응답 DTO 모음
public final class PointApiDto {
    private PointApiDto() {}

    // 포인트 충전 요청
    public record ChargeRequest(Long amount) {
        // 커맨드로 변환
        public PointCommand.Charge toCommand(long userId) {
            if (amount == null) {
                throw new DomainException(DomainErrorCode.NON_POSITIVE_MONEY);
            }
            return new PointCommand.Charge(userId, amount);
        }
    }

    // 포인트 잔액 응답
    public record BalanceResponse(long balance) {
        public static BalanceResponse from(PointResult result) {
            return new BalanceResponse(result.balance());
        }

        public static BalanceResponse from(long balance) {
            return new BalanceResponse(balance);
        }
    }
}
