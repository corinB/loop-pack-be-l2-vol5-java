package com.loopers.application.ordering.order;

import java.util.List;

public final class OrderCommand {
    private OrderCommand() {}

    public record Create(long userId, List<Item> items) {}

    public record Item(long productId, int quantity) {}
}
