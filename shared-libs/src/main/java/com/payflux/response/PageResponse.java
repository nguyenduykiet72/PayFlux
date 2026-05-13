package com.payflux.response;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long total) {
    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) total / size);
    }
}