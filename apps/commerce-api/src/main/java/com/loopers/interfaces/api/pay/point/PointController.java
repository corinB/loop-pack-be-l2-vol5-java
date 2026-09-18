package com.loopers.interfaces.api.pay.point;

import com.loopers.application.pay.point.ChargePointUseCase;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.support.XUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {
    private final ChargePointUseCase chargePointUseCase;

    @PostMapping("/charge")
    public ApiResponse<PointApiDto.BalanceResponse> charge(
        @XUserId long userId,
        @RequestBody PointApiDto.ChargeRequest request
    ) {
        return ApiResponse.success(PointApiDto.BalanceResponse.from(chargePointUseCase.execute(request.toCommand(userId))));
    }
}
