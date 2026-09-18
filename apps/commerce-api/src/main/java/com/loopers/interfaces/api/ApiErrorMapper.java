package com.loopers.interfaces.api;

import com.loopers.application.support.error.ApplicationErrorCode;
import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorMapper {

    public ErrorType map(DomainErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_USER_ID, INVALID_MONEY, NON_POSITIVE_MONEY, CALCULATION_OVERFLOW,
                 INVALID_STOCK, INVALID_QUANTITY, INVALID_NAME, INVALID_DESCRIPTION -> ErrorType.BAD_REQUEST;
            case DELETED_BRAND, DELETED_PRODUCT -> ErrorType.NOT_FOUND;
            case INSUFFICIENT_STOCK -> ErrorType.CONFLICT;
        };
    }

    public ErrorType map(ApplicationErrorCode errorCode) {
        return switch (errorCode) {
            case USER_NOT_FOUND, BRAND_NOT_FOUND, PRODUCT_NOT_FOUND -> ErrorType.NOT_FOUND;
            case BRAND_HAS_ACTIVE_PRODUCTS -> ErrorType.CONFLICT;
        };
    }
}
