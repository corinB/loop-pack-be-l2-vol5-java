package com.loopers.interfaces.api.ordering.order;

import com.loopers.application.ordering.order.CreateOrderUseCase;
import com.loopers.application.ordering.order.OrderView;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.support.XUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final CreateOrderUseCase createOrderUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderView>> create(
        @XUserId long userId,
        @RequestBody OrderApiDto.CreateRequest request
    ) {
        OrderView order = OrderView.from(createOrderUseCase.execute(request.toCommand(userId)));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(order));
    }
}
