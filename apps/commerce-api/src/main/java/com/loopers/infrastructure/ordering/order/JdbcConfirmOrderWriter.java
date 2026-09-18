package com.loopers.infrastructure.ordering.order;

import com.loopers.application.ordering.order.ConfirmOrderLoad;
import com.loopers.application.ordering.order.ConfirmOrderWriter;
import com.loopers.application.support.error.ApplicationErrorCode;
import com.loopers.application.support.error.ApplicationException;
import com.loopers.domain.mall.product.Product;
import com.loopers.domain.mall.product.ProductRepository;
import com.loopers.domain.ordering.order.Order;
import com.loopers.domain.ordering.order.OrderItem;
import com.loopers.domain.ordering.order.OrderRepository;
import com.loopers.domain.pay.orderbill.OrderBill;
import com.loopers.domain.pay.point.Point;
import com.loopers.domain.pay.point.PointBill;
import com.loopers.domain.pay.point.PointRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
// 주문 확정용 조회/저장 JDBC 구현체
public class JdbcConfirmOrderWriter implements ConfirmOrderWriter {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PointRepository pointRepository;
    private final JdbcClient jdbcClient;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    // 주문/상품/포인트를 함께 조회
    @Override
    public ConfirmOrderLoad load(long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.ORDER_NOT_FOUND));

        Map<Long, Product> productsByProductId = new LinkedHashMap<>();
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.PRODUCT_NOT_FOUND));
            productsByProductId.put(item.getProductId(), product);
        }

        Point point = pointRepository.findByUserId(order.getUserId()).orElseThrow();
        return new ConfirmOrderLoad(order, productsByProductId, point);
    }

    // 재고, 포인트, 결제 기록, 주문 상태를 한번에 저장
    @Override
    public void save(ConfirmOrderLoad load, PointBill pointBill, OrderBill orderBill) {
        saveProductStocks(load.productsByProductId().values());
        savePointBalance(load.point());
        savePointBill(pointBill);
        saveOrderBill(orderBill);
        saveOrderStatus(load.order());
    }

    // 상품 재고를 배치로 반영
    private void saveProductStocks(Collection<Product> products) {
        Instant now = Instant.now();
        SqlParameterSource[] batchArgs = products.stream()
            .map(product -> new MapSqlParameterSource()
                .addValue("id", product.getId())
                .addValue("stock", product.getStock())
                .addValue("updatedAt", now))
            .toArray(SqlParameterSource[]::new);
        namedParameterJdbcTemplate.batchUpdate(
            "UPDATE products SET stock = :stock, updated_at = :updatedAt WHERE id = :id",
            batchArgs
        );
    }

    // 포인트 잔액 갱신
    private void savePointBalance(Point point) {
        jdbcClient.sql("UPDATE points SET balance = :balance WHERE user_id = :userId")
            .param("balance", point.getBalance())
            .param("userId", point.getUserId())
            .update();
    }

    // 포인트 사용 내역 저장
    private void savePointBill(PointBill pointBill) {
        jdbcClient.sql("""
                INSERT INTO point_bills (user_id, type, amount, order_id, created_at)
                VALUES (:userId, :type, :amount, :orderId, :createdAt)
                """)
            .param("userId", pointBill.getUserId())
            .param("type", pointBill.getType().name())
            .param("amount", pointBill.getAmount())
            .param("orderId", pointBill.getOrderId())
            .param("createdAt", Instant.now())
            .update();
    }

    // 결제 기록 저장
    private void saveOrderBill(OrderBill orderBill) {
        jdbcClient.sql("""
                INSERT INTO order_bills (order_id, user_id, amount, status, created_at)
                VALUES (:orderId, :userId, :amount, :status, :createdAt)
                """)
            .param("orderId", orderBill.getOrderId())
            .param("userId", orderBill.getUserId())
            .param("amount", orderBill.getAmount())
            .param("status", orderBill.getStatus().name())
            .param("createdAt", Instant.now())
            .update();
    }

    // 주문 상태 저장
    private void saveOrderStatus(Order order) {
        jdbcClient.sql("UPDATE orders SET status = :status WHERE id = :id")
            .param("status", order.getStatus().name())
            .param("id", order.getId())
            .update();
    }
}
