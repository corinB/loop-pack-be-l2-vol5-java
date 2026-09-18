package com.loopers.application.ordering.order;

import com.loopers.domain.pay.orderbill.OrderBillStatus;

public record ConfirmOrderResult(OrderResult order, long paymentAmount, OrderBillStatus paymentStatus) {}
