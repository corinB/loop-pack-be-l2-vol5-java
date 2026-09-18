package com.loopers.interfaces.api.shopping.like;

import com.loopers.application.common.PageCriteria;
import com.loopers.application.common.PageResult;
import com.loopers.application.shopping.like.LikeItem;
import com.loopers.application.shopping.like.LikeQueryDao;
import com.loopers.application.shopping.user.UserQueryDao;
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
@RequestMapping("/api/v1/users/{userId}/likes")
@RequiredArgsConstructor
public class LikeQueryController {
    private final LikeQueryDao likeQueryDao;
    private final UserQueryDao userQueryDao;

    @GetMapping
    public ApiResponse<PageResult<LikeItem>> findAll(
        @PathVariable long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        RequestInputValidator.requirePositiveId(userId, "사용자 ID");
        userQueryDao.findById(userId).orElseThrow(() -> new ApplicationException(ApplicationErrorCode.USER_NOT_FOUND));
        return ApiResponse.success(likeQueryDao.findByUserId(userId, new PageCriteria(page, size)));
    }
}
