package com.loopers.application.common;

import java.util.List;

public record PageResult<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
    public PageResult {
        items = List.copyOf(items);
    }

    public static <T> PageResult<T> of(List<T> items, int page, int size, long totalElements) {
        int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + size - 1) / size);
        return new PageResult<>(items, page, size, totalElements, totalPages);
    }
}
