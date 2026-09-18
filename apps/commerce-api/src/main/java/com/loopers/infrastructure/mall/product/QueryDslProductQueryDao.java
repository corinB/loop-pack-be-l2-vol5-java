package com.loopers.infrastructure.mall.product;

import com.loopers.application.common.PageResult;
import com.loopers.application.mall.product.AdminProduct;
import com.loopers.application.mall.product.BrandSummary;
import com.loopers.application.mall.product.ProductCriteria;
import com.loopers.application.mall.product.ProductDetail;
import com.loopers.application.mall.product.ProductQueryDao;
import com.loopers.application.mall.product.ProductSort;
import com.loopers.application.mall.product.ProductSummary;
import com.loopers.infrastructure.mall.brand.QBrandJpaEntity;
import com.loopers.infrastructure.shopping.like.QProductLikeCountJpaEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class QueryDslProductQueryDao implements ProductQueryDao {
    private final JPAQueryFactory queryFactory;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductSummary> findProducts(ProductCriteria criteria) {
        return findPage(criteria, this::summary);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminProduct> findAdminProducts(ProductCriteria criteria) {
        return findPage(criteria, this::admin);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductDetail> findProduct(long productId) {
        return findTuple(productId).map(this::detail);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdminProduct> findAdminProduct(long productId) {
        return findTuple(productId).map(this::admin);
    }

    private <T> PageResult<T> findPage(ProductCriteria criteria, Function<Tuple, T> mapper) {
        QProductJpaEntity product = QProductJpaEntity.productJpaEntity;
        QBrandJpaEntity brand = QBrandJpaEntity.brandJpaEntity;
        QProductLikeCountJpaEntity likeCount = QProductLikeCountJpaEntity.productLikeCountJpaEntity;
        BooleanBuilder where = activeProducts(criteria, product, brand);

        Long total = queryFactory.select(product.count())
            .from(product)
            .join(brand).on(brand.id.eq(product.brandId))
            .where(where)
            .fetchOne();
        List<T> items = queryFactory.select(product.id, product.name, product.description, product.price,
                product.stock, product.createdAt, brand.id, brand.name, likeCount.likeCount.coalesce(0L))
            .from(product)
            .join(brand).on(brand.id.eq(product.brandId))
            .leftJoin(likeCount).on(likeCount.productId.eq(product.id))
            .where(where)
            .orderBy(orderBy(criteria.sort(), product, likeCount))
            .offset(criteria.page().offset())
            .limit(criteria.page().size())
            .fetch()
            .stream()
            .map(mapper)
            .toList();
        return PageResult.of(items, criteria.page().page(), criteria.page().size(), total == null ? 0L : total);
    }

    private Optional<Tuple> findTuple(long productId) {
        QProductJpaEntity product = QProductJpaEntity.productJpaEntity;
        QBrandJpaEntity brand = QBrandJpaEntity.brandJpaEntity;
        QProductLikeCountJpaEntity likeCount = QProductLikeCountJpaEntity.productLikeCountJpaEntity;
        Tuple tuple = queryFactory.select(product.id, product.name, product.description, product.price,
                product.stock, product.createdAt, brand.id, brand.name, likeCount.likeCount.coalesce(0L))
            .from(product)
            .join(brand).on(brand.id.eq(product.brandId).and(brand.deleted.isFalse()))
            .leftJoin(likeCount).on(likeCount.productId.eq(product.id))
            .where(product.id.eq(productId), product.deleted.isFalse())
            .fetchOne();
        return Optional.ofNullable(tuple);
    }

    private BooleanBuilder activeProducts(ProductCriteria criteria, QProductJpaEntity product,
                                          QBrandJpaEntity brand) {
        BooleanBuilder where = new BooleanBuilder()
            .and(product.deleted.isFalse())
            .and(brand.deleted.isFalse());
        if (criteria.brandId() != null) {
            where.and(product.brandId.eq(criteria.brandId()));
        }
        return where;
    }

    private OrderSpecifier<?>[] orderBy(ProductSort sort, QProductJpaEntity product,
                                        QProductLikeCountJpaEntity likeCount) {
        OrderSpecifier<?> primary = switch (sort) {
            case LATEST -> product.createdAt.desc();
            case PRICE_ASC -> product.price.asc();
            case LIKES_DESC -> likeCount.likeCount.coalesce(0L).desc();
        };
        return new OrderSpecifier<?>[] {primary, product.id.desc()};
    }

    private ProductSummary summary(Tuple tuple) {
        QProductJpaEntity product = QProductJpaEntity.productJpaEntity;
        QProductLikeCountJpaEntity likeCount = QProductLikeCountJpaEntity.productLikeCountJpaEntity;
        return new ProductSummary(value(tuple, product.id), value(tuple, product.name), value(tuple, product.price),
            brand(tuple), value(tuple, likeCount.likeCount.coalesce(0L)));
    }

    private ProductDetail detail(Tuple tuple) {
        QProductJpaEntity product = QProductJpaEntity.productJpaEntity;
        QProductLikeCountJpaEntity likeCount = QProductLikeCountJpaEntity.productLikeCountJpaEntity;
        return new ProductDetail(value(tuple, product.id), value(tuple, product.name), value(tuple, product.price),
            brand(tuple), value(tuple, likeCount.likeCount.coalesce(0L)), value(tuple, product.description),
            value(tuple, product.stock));
    }

    private AdminProduct admin(Tuple tuple) {
        QProductJpaEntity product = QProductJpaEntity.productJpaEntity;
        QProductLikeCountJpaEntity likeCount = QProductLikeCountJpaEntity.productLikeCountJpaEntity;
        return new AdminProduct(value(tuple, product.id), value(tuple, product.name), value(tuple, product.price),
            brand(tuple), value(tuple, likeCount.likeCount.coalesce(0L)), value(tuple, product.description),
            value(tuple, product.stock), value(tuple, product.createdAt));
    }

    private BrandSummary brand(Tuple tuple) {
        QBrandJpaEntity brand = QBrandJpaEntity.brandJpaEntity;
        return new BrandSummary(value(tuple, brand.id), value(tuple, brand.name));
    }

    private <T> T value(Tuple tuple, com.querydsl.core.types.Expression<T> expression) {
        return tuple.get(expression);
    }
}
