package com.loopers.interfaces.api.pay.point;

import com.loopers.application.pay.point.PointQueryDao;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.support.XUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
// 포인트 조회 API 컨트롤러
public class PointQueryController {
    private final PointQueryDao pointQueryDao;

    // 사용자 포인트 잔액 조회
    @GetMapping
    public ApiResponse<PointApiDto.BalanceResponse> findBalance(@XUserId long userId) {
        return ApiResponse.success(PointApiDto.BalanceResponse.from(pointQueryDao.findBalance(userId).orElseThrow()));
    }
}
