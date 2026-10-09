package com.JavaTraining.BaiTap_RS.library.patron.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class LibraryPatronPageableResolver {

    private static final List<String> SORTABLE_FIELDS = List.of("id", "joinedAt", "status", "userId");

    public Pageable resolve(int page, int size, String sortField, Sort.Direction direction) {
        if (invalidPage(page, size) || invalidSort(sortField)) {
            throw new LibraryPatronException(HttpStatus.BAD_REQUEST, "INVALID_LIBRARY_QUERY",
                    "Tham số phân trang hoặc sắp xếp không hợp lệ");
        }
        Sort sort = sortField == null ? Sort.by(Sort.Direction.ASC, "joinedAt")
                : Sort.by(direction == null ? Sort.Direction.ASC : direction, sortField);
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(page, size, sort);
    }

    public Pageable candidates(int page, int size) {
        if (invalidPage(page, size)) {
            throw new LibraryPatronException(HttpStatus.BAD_REQUEST, "INVALID_LIBRARY_QUERY",
                    "Tham số phân trang không hợp lệ");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
    }

    private boolean invalidPage(int page, int size) {
        return page < 0 || size < 1 || size > 100;
    }

    private boolean invalidSort(String sortField) {
        return sortField != null && !SORTABLE_FIELDS.contains(sortField);
    }
}
