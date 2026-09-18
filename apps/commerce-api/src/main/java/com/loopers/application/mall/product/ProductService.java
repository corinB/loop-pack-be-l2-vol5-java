package com.loopers.application.mall.product;

import com.loopers.application.support.error.ApplicationErrorCode;
import com.loopers.application.support.error.ApplicationException;
import com.loopers.domain.mall.brand.Brand;
import com.loopers.domain.mall.brand.BrandRepository;
import com.loopers.domain.mall.product.Product;
import com.loopers.domain.mall.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService implements CreateProductUseCase, UpdateProductUseCase, DeleteProductUseCase,
        SetProductStockUseCase {
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;

    @Override
    @Transactional
    public ProductResult execute(ProductCommand.Create command) {
        Brand brand = brandRepository.findById(command.brandId())
            .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.BRAND_NOT_FOUND));
        brand.ensureActive();
        Product product = Product.create(command.brandId(), command.name(), command.description(), command.price(),
            command.stock());
        return ProductResult.from(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResult execute(ProductCommand.Update command) {
        Product product = findProduct(command.productId());
        product.update(command.name(), command.description(), command.price());
        return ProductResult.from(productRepository.save(product));
    }

    @Override
    @Transactional
    public void execute(ProductCommand.Delete command) {
        Product product = findProduct(command.productId());
        product.delete();
        productRepository.save(product);
    }

    @Override
    @Transactional
    public ProductResult execute(ProductCommand.SetStock command) {
        Product product = findProduct(command.productId());
        product.setStock(command.stock());
        return ProductResult.from(productRepository.save(product));
    }

    private Product findProduct(long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.PRODUCT_NOT_FOUND));
    }
}
