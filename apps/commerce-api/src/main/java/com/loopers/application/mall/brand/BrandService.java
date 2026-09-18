package com.loopers.application.mall.brand;

import com.loopers.application.support.error.ApplicationErrorCode;
import com.loopers.application.support.error.ApplicationException;
import com.loopers.domain.mall.brand.Brand;
import com.loopers.domain.mall.brand.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrandService implements CreateBrandUseCase, UpdateBrandUseCase, DeleteBrandUseCase {
    private final BrandRepository brandRepository;
    private final ActiveProductChecker activeProductChecker;

    @Override
    @Transactional
    public BrandResult execute(BrandCommand.Create command) {
        return BrandResult.from(brandRepository.save(Brand.create(command.name(), command.description())));
    }

    @Override
    @Transactional
    public BrandResult execute(BrandCommand.Update command) {
        Brand brand = findBrand(command.brandId());
        brand.update(command.name(), command.description());
        return BrandResult.from(brandRepository.save(brand));
    }

    @Override
    @Transactional
    public void execute(BrandCommand.Delete command) {
        Brand brand = findBrand(command.brandId());
        brand.ensureActive();
        if (activeProductChecker.existsByBrandId(command.brandId())) {
            throw new ApplicationException(ApplicationErrorCode.BRAND_HAS_ACTIVE_PRODUCTS);
        }
        brand.delete();
        brandRepository.save(brand);
    }

    private Brand findBrand(long brandId) {
        return brandRepository.findById(brandId)
            .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.BRAND_NOT_FOUND));
    }
}
