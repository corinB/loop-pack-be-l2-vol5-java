package com.loopers.interfaces.api.ordering.order;

import com.loopers.application.ordering.order.OrderCommand;
import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;
import java.util.List;

public final class OrderApiDto {
    private OrderApiDto() {}

    public record ItemRequest(Long productId, Integer quantity) {
        OrderCommand.Item toCommandItem() {
            if (productId == null || quantity == null || productId <= 0 || quantity <= 0) {
                throw new DomainException(DomainErrorCode.EMPTY_ORDER_ITEMS);
            }
            return new OrderCommand.Item(productId, quantity);
        }
    }

    public record CreateRequest(List<ItemRequest> items) {
        public OrderCommand.Create toCommand(long userId) {
            if (items == null || items.isEmpty()) {
                throw new DomainException(DomainErrorCode.EMPTY_ORDER_ITEMS);
            }
            List<OrderCommand.Item> commandItems = items.stream().map(ItemRequest::toCommandItem).toList();
            return new OrderCommand.Create(userId, commandItems);
        }
    }
}
