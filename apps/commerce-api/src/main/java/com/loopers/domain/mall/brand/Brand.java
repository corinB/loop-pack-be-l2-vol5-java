package com.loopers.domain.mall.brand;

import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;
import java.time.Instant;

public final class Brand {
    private final Long id;
    private String name;
    private String description;
    private boolean deleted;
    private final Instant createdAt;

    private Brand(Long id, String name, String description, boolean deleted, Instant createdAt) {
        this.id = id;
        this.name = normalizeName(name);
        this.description = validateDescription(description);
        this.deleted = deleted;
        this.createdAt = createdAt;
    }

    public static Brand create(String name, String description) {
        return new Brand(null, name, description, false, null);
    }

    public static Brand restore(long id, String name, String description, boolean deleted, Instant createdAt) {
        if (id <= 0 || createdAt == null) {
            throw new IllegalArgumentException("저장된 브랜드 상태가 올바르지 않습니다.");
        }
        return new Brand(id, name, description, deleted, createdAt);
    }

    public void update(String name, String description) {
        ensureActive();
        String normalizedName = normalizeName(name);
        String validatedDescription = validateDescription(description);
        this.name = normalizedName;
        this.description = validatedDescription;
    }

    public void delete() {
        ensureActive();
        deleted = true;
    }

    public void ensureActive() {
        if (deleted) {
            throw new DomainException(DomainErrorCode.DELETED_BRAND);
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String normalizeName(String name) {
        if (name == null) {
            throw new DomainException(DomainErrorCode.INVALID_NAME);
        }
        String normalized = name.trim();
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw new DomainException(DomainErrorCode.INVALID_NAME);
        }
        return normalized;
    }

    private static String validateDescription(String description) {
        if (description != null && description.length() > 1_000) {
            throw new DomainException(DomainErrorCode.INVALID_DESCRIPTION);
        }
        return description;
    }
}
