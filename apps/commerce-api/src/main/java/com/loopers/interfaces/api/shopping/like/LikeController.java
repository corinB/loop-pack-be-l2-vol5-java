package com.loopers.interfaces.api.shopping.like;

import com.loopers.application.shopping.like.LikeCommandDao;
import com.loopers.application.support.error.ApplicationErrorCode;
import com.loopers.application.support.error.ApplicationException;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.support.RequestInputValidator;
import com.loopers.interfaces.api.support.XUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products/{productId}/likes")
@RequiredArgsConstructor
public class LikeController {
    private final LikeCommandDao likeCommandDao;

    @PostMapping
    public ApiResponse<Object> register(@XUserId long userId, @PathVariable long productId) {
        RequestInputValidator.requirePositiveId(productId, "상품 ID");
        if (!likeCommandDao.existsActiveProduct(productId)) {
            throw new ApplicationException(ApplicationErrorCode.PRODUCT_NOT_FOUND);
        }
        likeCommandDao.register(userId, productId);
        return ApiResponse.success();
    }

    @DeleteMapping
    public ApiResponse<Object> cancel(@XUserId long userId, @PathVariable long productId) {
        RequestInputValidator.requirePositiveId(productId, "상품 ID");
        likeCommandDao.cancel(userId, productId);
        return ApiResponse.success();
    }
}
