package com.loopers.interfaces.api.mall.product;

import com.loopers.application.common.PageCriteria;
import com.loopers.application.common.PageResult;
import com.loopers.application.mall.product.AdminProduct;
import com.loopers.application.mall.product.CreateProductUseCase;
import com.loopers.application.mall.product.DeleteProductUseCase;
import com.loopers.application.mall.product.ProductCommand;
import com.loopers.application.mall.product.ProductCriteria;
import com.loopers.application.mall.product.ProductQueryDao;
import com.loopers.application.mall.product.ProductSort;
import com.loopers.application.mall.product.SetProductStockUseCase;
import com.loopers.application.mall.product.UpdateProductUseCase;
import com.loopers.application.support.error.ApplicationErrorCode;
import com.loopers.application.support.error.ApplicationException;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.support.RequestInputValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api-admin/v1/products")
@RequiredArgsConstructor
public class AdminProductController {
    private final ProductQueryDao productQueryDao;
    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final SetProductStockUseCase setProductStockUseCase;

    @GetMapping
    public ApiResponse<PageResult<AdminProduct>> findAll(
        @RequestParam(required = false) Long brandId,
        @RequestParam(defaultValue = "latest") String sort,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        ProductCriteria criteria = new ProductCriteria(brandId, ProductSort.from(sort), new PageCriteria(page, size));
        return ApiResponse.success(productQueryDao.findAdminProducts(criteria));
    }

    @GetMapping("/{productId}")
    public ApiResponse<AdminProduct> find(@PathVariable long productId) {
        RequestInputValidator.requirePositiveId(productId, "상품 ID");
        return ApiResponse.success(productQueryDao.findAdminProduct(productId).orElseThrow(AdminProductController::notFound));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminProduct>> create(@RequestBody ProductApiDto.CreateRequest request) {
        AdminProduct product = AdminProduct.from(createProductUseCase.execute(request.toCommand()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(product));
    }

    @PutMapping("/{productId}")
    public ApiResponse<AdminProduct> update(@PathVariable long productId, @RequestBody ProductApiDto.UpdateRequest request) {
        RequestInputValidator.requirePositiveId(productId, "상품 ID");
        return ApiResponse.success(AdminProduct.from(updateProductUseCase.execute(request.toCommand(productId))));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Object> delete(@PathVariable long productId) {
        RequestInputValidator.requirePositiveId(productId, "상품 ID");
        deleteProductUseCase.execute(new ProductCommand.Delete(productId));
        return ApiResponse.success();
    }

    @PutMapping("/{productId}/stock")
    public ApiResponse<AdminProduct> setStock(@PathVariable long productId, @RequestBody ProductApiDto.StockRequest request) {
        RequestInputValidator.requirePositiveId(productId, "상품 ID");
        return ApiResponse.success(AdminProduct.from(setProductStockUseCase.execute(request.toCommand(productId))));
    }

    private static ApplicationException notFound() {
        return new ApplicationException(ApplicationErrorCode.PRODUCT_NOT_FOUND);
    }
}
