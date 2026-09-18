package com.loopers.interfaces.api.ordering.order;

import com.loopers.application.common.PageCriteria;
import com.loopers.application.common.PageResult;
import com.loopers.application.ordering.order.AdminOrderView;
import com.loopers.application.ordering.order.OrderQueryDao;
import com.loopers.application.support.error.ApplicationErrorCode;
import com.loopers.application.support.error.ApplicationException;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.support.RequestInputValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api-admin/v1/orders")
@RequiredArgsConstructor
public class AdminOrderQueryController {
    private final OrderQueryDao orderQueryDao;

    @GetMapping
    public ApiResponse<PageResult<AdminOrderView>> findAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(orderQueryDao.findAdminOrders(new PageCriteria(page, size)));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<AdminOrderView> find(@PathVariable long orderId) {
        RequestInputValidator.requirePositiveId(orderId, "주문 ID");
        AdminOrderView order = orderQueryDao.findAdminOrder(orderId)
            .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.ORDER_NOT_FOUND));
        return ApiResponse.success(order);
    }
}
