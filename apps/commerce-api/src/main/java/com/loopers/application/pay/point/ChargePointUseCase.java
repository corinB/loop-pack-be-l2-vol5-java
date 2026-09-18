package com.loopers.application.pay.point;

// 포인트 충전 유스케이스
public interface ChargePointUseCase {
    PointResult execute(PointCommand.Charge command);
}
