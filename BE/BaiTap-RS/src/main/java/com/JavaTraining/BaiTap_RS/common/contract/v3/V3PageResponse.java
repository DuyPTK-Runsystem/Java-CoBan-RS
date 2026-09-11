package com.JavaTraining.BaiTap_RS.common.contract.v3;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Page payload reserved for new v3 list endpoints. Existing v2 page payloads are unchanged.
 *
 * @param <T> item type exposed by a capability-specific response DTO
 */
public record V3PageResponse<T>(
        List<T> items,
        int page,
        int pageSize,
        long total,
        Map<String, String> appliedFilters) {

    public V3PageResponse {
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or greater");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be greater than zero");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total must be zero or greater");
        }
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        appliedFilters = Map.copyOf(Objects.requireNonNull(appliedFilters, "appliedFilters must not be null"));
    }
}
