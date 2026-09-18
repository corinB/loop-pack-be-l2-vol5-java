package com.loopers.application.ordering.order;

import com.loopers.domain.mall.product.Product;
import com.loopers.domain.ordering.order.Order;
import com.loopers.domain.ordering.order.OrderItem;
import com.loopers.domain.pay.orderbill.OrderBill;
import com.loopers.domain.pay.orderbill.OrderBillStatus;
import com.loopers.domain.pay.point.Point;
import com.loopers.domain.pay.point.PointBill;
import com.loopers.domain.shared.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfirmOrderService implements ConfirmOrderUseCase {
    private final ConfirmOrderWriter confirmOrderWriter;

    @Override
    @Transactional
    public ConfirmOrderResult execute(ConfirmOrderCommand command) {
        ConfirmOrderLoad load = confirmOrderWriter.load(command.orderId());
        Order order = load.order();
        order.confirm();

        for (OrderItem item : order.getItems()) {
            Product product = load.productsByProductId().get(item.getProductId());
            product.ensureActive();
            product.decreaseStock(item.getQuantity());
        }

        Point point = load.point();
        point.use(Money.positive(order.getTotalAmount()));

        PointBill pointBill = PointBill.use(order.getUserId(), order.getId(), order.getTotalAmount());
        OrderBill orderBill = OrderBill.paid(order.getId(), order.getUserId(), order.getTotalAmount());
        confirmOrderWriter.save(load, pointBill, orderBill);

        return new ConfirmOrderResult(OrderResult.from(order), order.getTotalAmount(), OrderBillStatus.PAID);
    }
}
