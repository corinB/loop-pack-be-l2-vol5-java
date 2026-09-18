package com.loopers.interfaces.api.mall.product;

import com.loopers.application.common.PageCriteria;
import com.loopers.application.common.PageResult;
import com.loopers.application.mall.product.ProductCriteria;
import com.loopers.application.mall.product.ProductDetail;
import com.loopers.application.mall.product.ProductQueryDao;
import com.loopers.application.mall.product.ProductSort;
import com.loopers.application.mall.product.ProductSummary;
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
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
// 상품 조회 전용 컨트롤러
public class ProductQueryController {
    private final ProductQueryDao productQueryDao;

    // 상품 목록 조회
    @GetMapping
    public ApiResponse<PageResult<ProductSummary>> findAll(
        @RequestParam(required = false) Long brandId,
        @RequestParam(defaultValue = "latest") String sort,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        ProductCriteria criteria = new ProductCriteria(brandId, ProductSort.from(sort), new PageCriteria(page, size));
        return ApiResponse.success(productQueryDao.findProducts(criteria));
    }

    // 상품 단건 조회
    @GetMapping("/{productId}")
    public ApiResponse<ProductDetail> find(@PathVariable long productId) {
        RequestInputValidator.requirePositiveId(productId, "상품 ID");
        ProductDetail product = productQueryDao.findProduct(productId)
            .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.PRODUCT_NOT_FOUND));
        return ApiResponse.success(product);
    }
}
