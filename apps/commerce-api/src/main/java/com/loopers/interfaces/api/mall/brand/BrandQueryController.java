package com.loopers.interfaces.api.mall.brand;

import com.loopers.application.mall.brand.BrandDetail;
import com.loopers.application.mall.brand.BrandQueryDao;
import com.loopers.application.support.error.ApplicationErrorCode;
import com.loopers.application.support.error.ApplicationException;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandQueryController {
    private final BrandQueryDao brandQueryDao;

    @GetMapping("/{brandId}")
    public ApiResponse<BrandDetail> find(@PathVariable long brandId) {
        BrandDetail detail = brandQueryDao.findById(brandId)
            .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.BRAND_NOT_FOUND));
        return ApiResponse.success(detail);
    }
}
