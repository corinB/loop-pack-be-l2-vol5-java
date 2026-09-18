package com.loopers.application.shopping.like;

import com.loopers.application.mall.product.BrandSummary;
import java.time.Instant;

// 좋아요 목록 조회 결과 항목
public record LikeItem(long productId, String name, long price, BrandSummary brand, long likeCount, Instant likedAt) {}
