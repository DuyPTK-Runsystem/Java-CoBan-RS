package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryCatalogException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class BookSortResolverTest {

    private static final String INVALID_QUERY_CODE = "INVALID_CATALOG_QUERY";
    private final BookSortResolver resolver = new BookSortResolver();

    @Test
    void defaultsToTitleAndStableIdOrder() {
        Pageable pageable = resolver.resolve(0, 20, List.of());

        assertEquals(List.of(0, 20, List.of(Sort.Order.asc("title"), Sort.Order.asc("id"))),
                List.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().toList()),
                "Default pagination and sort order should be stable");
    }

    @Test
    void preservesPageBoundaryAndAppendsStableIdOrder() {
        Pageable pageable = resolver.resolve(3, 100, List.of("author,desc"));

        assertEquals(List.of(3, 100, List.of(Sort.Order.desc("author"), Sort.Order.asc("id"))),
                List.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().toList()),
                "Requested pagination and sort order should be preserved");
    }

    @Test
    void copySortUsesCopyFieldsAndStableIdOrder() {
        Pageable pageable = resolver.resolveCopies(0, 20, List.of("barcode,asc"));

        assertEquals(List.of(Sort.Order.asc("barcode"), Sort.Order.asc("id")), pageable.getSort().toList(),
                "Copy sorting should accept barcode and append the stable ID");
    }

    @Test
    void rejectsOutOfBoundsPagination() throws Throwable {
        assertEquals(List.of(INVALID_QUERY_CODE, INVALID_QUERY_CODE),
                List.of(catalogErrorCode(() -> resolver.resolve(-1, 20, List.of())),
                        catalogErrorCode(() -> resolver.resolve(0, 101, List.of()))),
                "Both pagination boundary violations should use the catalog query error code");
    }

    @Test
    void rejectsUnknownFieldsAndDirectionsWithStableCode() throws Throwable {
        assertEquals(List.of(INVALID_QUERY_CODE, INVALID_QUERY_CODE, INVALID_QUERY_CODE),
                List.of(catalogErrorCode(() -> resolver.resolve(0, 20, List.of("publisher,asc"))),
                        catalogErrorCode(() -> resolver.resolveCopies(0, 20, List.of("title,asc"))),
                        catalogErrorCode(() -> resolver.resolve(0, 20, List.of("title,sideways")))),
                "Invalid fields and directions should use the catalog query error code");
    }

    private String catalogErrorCode(Executable action) throws Throwable {
        try {
            action.execute();
        } catch (LibraryCatalogException error) {
            return error.getCode();
        }
        throw new AssertionError("Invalid catalog query should be rejected");
    }
}
