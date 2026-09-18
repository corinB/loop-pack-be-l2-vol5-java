package com.loopers.application.pay.point;

public final class PointCommand {
    private PointCommand() {}

    public record Charge(long userId, long amount) {}
}
