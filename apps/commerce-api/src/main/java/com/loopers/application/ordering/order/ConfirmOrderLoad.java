package com.loopers.application.ordering.order;

import com.loopers.domain.mall.product.Product;
import com.loopers.domain.ordering.order.Order;
import com.loopers.domain.pay.point.Point;
import java.util.Map;

public record ConfirmOrderLoad(Order order, Map<Long, Product> productsByProductId, Point point) {}
