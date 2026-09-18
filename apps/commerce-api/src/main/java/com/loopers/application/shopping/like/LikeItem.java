package com.loopers.application.shopping.like;

import com.loopers.application.mall.product.BrandSummary;
import java.time.Instant;

public record LikeItem(long productId, String name, long price, BrandSummary brand, long likeCount, Instant likedAt) {}
