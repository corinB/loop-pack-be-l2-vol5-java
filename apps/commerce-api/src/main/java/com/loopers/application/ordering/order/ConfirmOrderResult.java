package com.loopers.application.ordering.order;

import com.loopers.domain.pay.orderbill.OrderBillStatus;

// 주문 확정 결과
public record ConfirmOrderResult(OrderResult order, long paymentAmount, OrderBillStatus paymentStatus) {}
