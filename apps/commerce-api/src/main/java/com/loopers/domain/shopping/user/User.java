package com.loopers.domain.shopping.user;

import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;

public final class User {
    private final long id;

    private User(long id) {
        if (id <= 0) {
            throw new DomainException(DomainErrorCode.INVALID_USER_ID);
        }
        this.id = id;
    }

    public static User create(long id) {
        return new User(id);
    }

    public static User restore(long id) {
        return new User(id);
    }

    public long getId() {
        return id;
    }
}
