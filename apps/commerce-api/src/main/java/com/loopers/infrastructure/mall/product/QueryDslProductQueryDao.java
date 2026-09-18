package com.loopers.infrastructure.mall.product;

import com.loopers.application.common.PageResult;
import com.loopers.application.mall.product.AdminProduct;
import com.loopers.application.mall.product.ProductCriteria;
import com.loopers.application.mall.product.ProductDetail;
import com.loopers.application.mall.product.ProductQueryDao;
import com.loopers.application.mall.product.ProductSort;
import com.loopers.application.mall.product.ProductSummary;
import com.loopers.infrastructure.mall.brand.QBrandJpaEntity;
import com.loopers.infrastructure.shopping.like.QProductLikeCountJpaEntity;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
// QueryDSL 기반 상품 조회 DAO
public class QueryDslProductQueryDao implements ProductQueryDao {
    private static final QProductJpaEntity PRODUCT = QProductJpaEntity.productJpaEntity;
    private static final QBrandJpaEntity BRAND = QBrandJpaEntity.brandJpaEntity;
    private static final QProductLikeCountJpaEntity LIKE_COUNT_ROW =
        QProductLikeCountJpaEntity.productLikeCountJpaEntity;
    private static final NumberExpression<Long> LIKE_COUNT = LIKE_COUNT_ROW.likeCount.coalesce(0L);

    private final JPAQueryFactory queryFactory;

    // 상품 목록 페이지 조회
    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductSummary> findProducts(ProductCriteria criteria) {
        return findPage(criteria, ProductQueryRow::toSummary);
    }

    // 관리자용 상품 목록 페이지 조회
    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminProduct> findAdminProducts(ProductCriteria criteria) {
        return findPage(criteria, ProductQueryRow::toAdminProduct);
    }

    // 상품 상세 조회
    @Override
    @Transactional(readOnly = true)
    public Optional<ProductDetail> findProduct(long productId) {
        return findRow(productId).map(ProductQueryRow::toDetail);
    }

    // 관리자용 상품 상세 조회
    @Override
    @Transactional(readOnly = true)
    public Optional<AdminProduct> findAdminProduct(long productId) {
        return findRow(productId).map(ProductQueryRow::toAdminProduct);
    }

    private <T> PageResult<T> findPage(ProductCriteria criteria, Function<ProductQueryRow, T> mapper) {
        List<T> items = selectProducts()
            .where(activeProduct(), activeBrand(), brandIdEquals(criteria.brandId()))
            .orderBy(orderBy(criteria.sort()))
            .offset(criteria.page().offset())
            .limit(criteria.page().size())
            .fetch()
            .stream()
            .map(mapper)
            .toList();
        return PageResult.of(items, criteria.page().page(), criteria.page().size(), countProducts(criteria));
    }

    private Optional<ProductQueryRow> findRow(long productId) {
        ProductQueryRow row = selectProducts()
            .where(PRODUCT.id.eq(productId), activeProduct(), activeBrand())
            .fetchOne();
        return Optional.ofNullable(row);
    }

    private JPAQuery<ProductQueryRow> selectProducts() {
        return queryFactory.select(productProjection())
            .from(PRODUCT)
            .join(BRAND).on(BRAND.id.eq(PRODUCT.brandId))
            .leftJoin(LIKE_COUNT_ROW).on(LIKE_COUNT_ROW.productId.eq(PRODUCT.id));
    }

    private ConstructorExpression<ProductQueryRow> productProjection() {
        return Projections.constructor(ProductQueryRow.class, PRODUCT.id, PRODUCT.name, PRODUCT.price,
            BRAND.id, BRAND.name, LIKE_COUNT, PRODUCT.description, PRODUCT.stock, PRODUCT.createdAt);
    }

    private long countProducts(ProductCriteria criteria) {
        Long count = queryFactory.select(PRODUCT.count())
            .from(PRODUCT)
            .join(BRAND).on(BRAND.id.eq(PRODUCT.brandId))
            .where(activeProduct(), activeBrand(), brandIdEquals(criteria.brandId()))
            .fetchOne();
        return count == null ? 0L : count;
    }

    private BooleanExpression activeProduct() {
        return PRODUCT.deleted.isFalse();
    }

    private BooleanExpression activeBrand() {
        return BRAND.deleted.isFalse();
    }

    private BooleanExpression brandIdEquals(Long brandId) {
        return brandId == null ? null : PRODUCT.brandId.eq(brandId);
    }

    private OrderSpecifier<?>[] orderBy(ProductSort sort) {
        OrderSpecifier<?> primary = switch (sort) {
            case LATEST -> PRODUCT.createdAt.desc();
            case PRICE_ASC -> PRODUCT.price.asc();
            case LIKES_DESC -> LIKE_COUNT.desc();
        };
        return new OrderSpecifier<?>[] {primary, PRODUCT.id.desc()};
    }
}
