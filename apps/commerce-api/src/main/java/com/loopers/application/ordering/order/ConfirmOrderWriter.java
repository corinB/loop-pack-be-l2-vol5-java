package com.loopers.application.ordering.order;

import com.loopers.domain.pay.orderbill.OrderBill;
import com.loopers.domain.pay.point.PointBill;

public interface ConfirmOrderWriter {
    ConfirmOrderLoad load(long orderId);

    void save(ConfirmOrderLoad load, PointBill pointBill, OrderBill orderBill);
}
