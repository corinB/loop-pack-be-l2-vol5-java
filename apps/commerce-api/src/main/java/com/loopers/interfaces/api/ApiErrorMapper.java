package com.loopers.interfaces.api;

import com.loopers.application.support.error.ApplicationErrorCode;
import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorMapper {

    public ErrorType map(DomainErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_USER_ID -> ErrorType.BAD_REQUEST;
        };
    }

    public ErrorType map(ApplicationErrorCode errorCode) {
        return switch (errorCode) {
            case USER_NOT_FOUND -> ErrorType.NOT_FOUND;
        };
    }
}
