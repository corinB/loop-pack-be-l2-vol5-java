package com.loopers.interfaces.api.mall.brand;

import com.loopers.application.mall.brand.BrandDetail;
import com.loopers.application.mall.brand.BrandQueryDao;
import com.loopers.application.support.error.ApplicationErrorCode;
import com.loopers.application.support.error.ApplicationException;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.support.RequestInputValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
// 브랜드 조회 전용 컨트롤러
public class BrandQueryController {
    private final BrandQueryDao brandQueryDao;

    // 브랜드 단건 조회
    @GetMapping("/{brandId}")
    public ApiResponse<BrandDetail> find(@PathVariable long brandId) {
        RequestInputValidator.requirePositiveId(brandId, "브랜드 ID");
        BrandDetail detail = brandQueryDao.findById(brandId)
            .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.BRAND_NOT_FOUND));
        return ApiResponse.success(detail);
    }
}
