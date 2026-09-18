package com.loopers.application.mall.product;

import com.loopers.application.common.PageResult;
import java.util.Optional;

public interface ProductQueryDao {
    PageResult<ProductSummary> findProducts(ProductCriteria criteria);

    PageResult<AdminProduct> findAdminProducts(ProductCriteria criteria);

    Optional<ProductDetail> findProduct(long productId);

    Optional<AdminProduct> findAdminProduct(long productId);
}
