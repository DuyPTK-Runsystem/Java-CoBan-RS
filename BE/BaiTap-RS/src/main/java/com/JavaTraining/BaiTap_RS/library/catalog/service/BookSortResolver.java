package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.util.List;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryCatalogException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class BookSortResolver {

    private static final Set<String> ALLOWED_SORTS = Set.of("title", "author", "publishedYear", "listPrice", "id");
    private static final Set<String> ALLOWED_COPY_SORTS = Set.of("barcode", "shelfLocation", "status", "id");

    public Pageable resolve(int page, int size, List<String> sortParameters) {
        return resolve(page, size, sortParameters, ALLOWED_SORTS, "title");
    }

    public Pageable resolveCopies(int page, int size, List<String> sortParameters) {
        return resolve(page, size, sortParameters, ALLOWED_COPY_SORTS, "barcode");
    }

    private Pageable resolve(int page, int size, List<String> sortParameters, Set<String> allowed,
            String defaultSort) {
        if (page < 0 || size < 1 || size > 100) {
            throw new LibraryCatalogException(HttpStatus.BAD_REQUEST, "INVALID_CATALOG_QUERY",
                    "page must be non-negative and size must be between 1 and 100");
        }
        Sort sort = Sort.unsorted();
        for (String parameter : sortParameters) {
            sort = sort.and(parseOrder(parameter, allowed));
        }
        if (sort.isUnsorted()) {
            sort = Sort.by(Sort.Direction.ASC, defaultSort);
        }
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(page, size, sort);
    }

    private Sort parseOrder(String parameter, Set<String> allowed) {
        String[] tokens = parameter.split(",", -1);
        String property = tokens[0];
        if (!allowed.contains(property) || tokens.length > 2) {
            throw new LibraryCatalogException(HttpStatus.BAD_REQUEST, "INVALID_CATALOG_QUERY",
                    "Unsupported sort field");
        }
        Sort.Direction direction = tokens.length == 2 ? parseDirection(tokens[1]) : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    private Sort.Direction parseDirection(String value) {
        return switch (value.toUpperCase(java.util.Locale.ROOT)) {
            case "ASC" -> Sort.Direction.ASC;
            case "DESC" -> Sort.Direction.DESC;
            default -> throw new LibraryCatalogException(HttpStatus.BAD_REQUEST,
                    "INVALID_CATALOG_QUERY", "Unsupported sort direction");
        };
    }
}
