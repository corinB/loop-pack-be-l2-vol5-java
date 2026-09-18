package com.loopers.infrastructure.ordering.order;

import com.loopers.application.common.PageCriteria;
import com.loopers.application.common.PageResult;
import com.loopers.application.ordering.order.AdminOrderView;
import com.loopers.application.ordering.order.OrderItemView;
import com.loopers.application.ordering.order.OrderQueryDao;
import com.loopers.application.ordering.order.OrderView;
import com.loopers.domain.ordering.order.OrderStatus;
import com.loopers.domain.pay.orderbill.OrderBillStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JdbcOrderQueryDao implements OrderQueryDao {
    private final JdbcClient jdbcClient;

    @Override
    @Transactional(readOnly = true)
    public PageResult<OrderView> findOrders(long userId, PageCriteria criteria) {
        long total = jdbcClient.sql("SELECT COUNT(*) FROM orders WHERE user_id = :userId")
            .param("userId", userId)
            .query(Long.class)
            .single();

        List<OrderHeaderRow> headers = jdbcClient.sql("""
                SELECT o.id AS order_id, o.user_id, o.status, o.total_amount, o.created_at,
                       ob.amount AS payment_amount, ob.status AS payment_status
                FROM orders o
                LEFT JOIN order_bills ob ON ob.order_id = o.id
                WHERE o.user_id = :userId
                ORDER BY o.created_at DESC, o.id DESC
                LIMIT :size OFFSET :offset
                """)
            .param("userId", userId)
            .param("size", criteria.size())
            .param("offset", criteria.offset())
            .query(this::mapHeader)
            .list();

        Map<Long, List<OrderItemView>> itemsByOrderId = findItemsByOrderIds(orderIdsOf(headers));
        List<OrderView> items = headers.stream()
            .map(header -> header.toView(itemsByOrderId.getOrDefault(header.orderId(), List.of())))
            .toList();
        return PageResult.of(items, criteria.page(), criteria.size(), total);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderView> findOrder(long orderId) {
        return findHeader(orderId).map(header -> header.toView(findItemsByOrderId(orderId)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminOrderView> findAdminOrders(PageCriteria criteria) {
        long total = jdbcClient.sql("SELECT COUNT(*) FROM orders").query(Long.class).single();

        List<OrderHeaderRow> headers = jdbcClient.sql("""
                SELECT o.id AS order_id, o.user_id, o.status, o.total_amount, o.created_at,
                       ob.amount AS payment_amount, ob.status AS payment_status
                FROM orders o
                LEFT JOIN order_bills ob ON ob.order_id = o.id
                ORDER BY o.created_at DESC, o.id DESC
                LIMIT :size OFFSET :offset
                """)
            .param("size", criteria.size())
            .param("offset", criteria.offset())
            .query(this::mapHeader)
            .list();

        Map<Long, List<OrderItemView>> itemsByOrderId = findItemsByOrderIds(orderIdsOf(headers));
        List<AdminOrderView> items = headers.stream()
            .map(header -> header.toAdminView(itemsByOrderId.getOrDefault(header.orderId(), List.of())))
            .toList();
        return PageResult.of(items, criteria.page(), criteria.size(), total);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdminOrderView> findAdminOrder(long orderId) {
        return findHeader(orderId).map(header -> header.toAdminView(findItemsByOrderId(orderId)));
    }

    private Optional<OrderHeaderRow> findHeader(long orderId) {
        return jdbcClient.sql("""
                SELECT o.id AS order_id, o.user_id, o.status, o.total_amount, o.created_at,
                       ob.amount AS payment_amount, ob.status AS payment_status
                FROM orders o
                LEFT JOIN order_bills ob ON ob.order_id = o.id
                WHERE o.id = :orderId
                """)
            .param("orderId", orderId)
            .query(this::mapHeader)
            .optional();
    }

    private List<OrderItemView> findItemsByOrderId(long orderId) {
        return jdbcClient.sql("""
                SELECT order_id, product_id, product_name, unit_price, quantity, amount
                FROM order_items WHERE order_id = :orderId ORDER BY id ASC
                """)
            .param("orderId", orderId)
            .query(this::mapItem)
            .list()
            .stream()
            .map(OrderItemRow::toView)
            .toList();
    }

    private List<Long> orderIdsOf(List<OrderHeaderRow> headers) {
        return headers.stream().map(OrderHeaderRow::orderId).toList();
    }

    private Map<Long, List<OrderItemView>> findItemsByOrderIds(List<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        List<OrderItemRow> rows = jdbcClient.sql("""
                SELECT order_id, product_id, product_name, unit_price, quantity, amount
                FROM order_items WHERE order_id IN (:orderIds) ORDER BY order_id ASC, id ASC
                """)
            .param("orderIds", orderIds)
            .query(this::mapItem)
            .list();
        return rows.stream()
            .collect(Collectors.groupingBy(OrderItemRow::orderId,
                Collectors.mapping(OrderItemRow::toView, Collectors.toList())));
    }

    private OrderHeaderRow mapHeader(ResultSet rs, int rowNum) throws SQLException {
        long paymentAmountValue = rs.getLong("payment_amount");
        Long paymentAmount = rs.wasNull() ? null : paymentAmountValue;
        String paymentStatus = rs.getString("payment_status");
        return new OrderHeaderRow(
            rs.getLong("order_id"),
            rs.getLong("user_id"),
            OrderStatus.valueOf(rs.getString("status")),
            rs.getLong("total_amount"),
            paymentAmount,
            paymentStatus == null ? null : OrderBillStatus.valueOf(paymentStatus),
            rs.getTimestamp("created_at").toInstant()
        );
    }

    private OrderItemRow mapItem(ResultSet rs, int rowNum) throws SQLException {
        return new OrderItemRow(
            rs.getLong("order_id"),
            rs.getLong("product_id"),
            rs.getString("product_name"),
            rs.getLong("unit_price"),
            rs.getInt("quantity"),
            rs.getLong("amount")
        );
    }
}
