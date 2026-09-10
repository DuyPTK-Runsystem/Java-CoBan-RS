package com.JavaTraining.BaiTap_RS.common.contract;

import java.util.List;

/** Canonical zero-based pagination envelope for new list endpoints. */
public record ResultPaginationDTO<T>(Meta meta, List<T> result) {
    public record Meta(int page, int pageSize, int totalPages, long totalItems) { }
}
