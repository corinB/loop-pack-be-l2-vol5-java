package com.loopers.application.ordering.order;

import com.loopers.application.common.PageCriteria;
import com.loopers.application.common.PageResult;
import java.util.Optional;

public interface OrderQueryDao {
    PageResult<OrderView> findOrders(long userId, PageCriteria criteria);

    Optional<OrderView> findOrder(long orderId);

    PageResult<AdminOrderView> findAdminOrders(PageCriteria criteria);

    Optional<AdminOrderView> findAdminOrder(long orderId);
}
