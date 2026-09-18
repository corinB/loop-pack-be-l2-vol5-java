package com.loopers.application.pay.point;

// 포인트 관련 커맨드 모음
public final class PointCommand {
    private PointCommand() {}

    public record Charge(long userId, long amount) {}
}
