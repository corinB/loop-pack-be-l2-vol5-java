package com.loopers.application.pay.point;

public interface ChargePointUseCase {
    PointResult execute(PointCommand.Charge command);
}
