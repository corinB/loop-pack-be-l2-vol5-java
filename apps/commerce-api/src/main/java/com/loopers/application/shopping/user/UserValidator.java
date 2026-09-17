package com.loopers.application.shopping.user;

import com.loopers.application.support.error.ApplicationErrorCode;
import com.loopers.application.support.error.ApplicationException;
import com.loopers.domain.shopping.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserValidator {
    private final UserRepository userRepository;

    public void validate(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ApplicationException(ApplicationErrorCode.USER_NOT_FOUND);
        }
    }
}
