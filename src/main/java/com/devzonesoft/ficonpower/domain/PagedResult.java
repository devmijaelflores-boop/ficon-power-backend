package com.devzonesoft.ficonpower.domain;

import java.util.List;

public record PagedResult<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext
) {
    public static <T> PagedResult<T> of(List<T> items, int page, int size, long totalItems) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalItems / size);
        return new PagedResult<>(
                List.copyOf(items),
                page,
                size,
                totalItems,
                totalPages,
                page > 0 && totalPages > 0,
                page + 1 < totalPages
        );
    }
}
