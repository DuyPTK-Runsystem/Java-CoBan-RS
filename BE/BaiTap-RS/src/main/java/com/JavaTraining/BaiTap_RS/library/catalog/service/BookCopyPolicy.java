package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryCatalogException;
import org.springframework.http.HttpStatus;

public final class BookCopyPolicy {

    private static final List<BookCopyStatus> CIRCULATING_STATES = List.of(
            BookCopyStatus.ON_LOAN, BookCopyStatus.RESERVED, BookCopyStatus.LOST);

    private BookCopyPolicy() {
    }

    public static void ensureBookActive(Book book) {
        if (book.isArchived()) {
            throw new LibraryCatalogException(HttpStatus.CONFLICT, "BOOK_ARCHIVED", "Book is archived");
        }
    }

    public static void ensureEditable(BookCopy copy) {
        ensureBookActive(copy.getBook());
        if (CIRCULATING_STATES.contains(copy.getStatus())) {
            throw stateConflict("Copy cannot be changed while on loan, reserved, or lost");
        }
        if (copy.getStatus() == BookCopyStatus.WITHDRAWN) {
            throw stateConflict("Withdrawn copy is terminal");
        }
    }

    public static boolean isAllowedStatusChange(BookCopyStatus from, BookCopyStatus to) {
        return from == BookCopyStatus.AVAILABLE && to == BookCopyStatus.DAMAGED
                || from == BookCopyStatus.DAMAGED && to == BookCopyStatus.AVAILABLE;
    }

    public static void requireVersion(Long current, Long expected) {
        if (!current.equals(expected)) {
            throw new LibraryCatalogException(HttpStatus.CONFLICT, "VERSION_CONFLICT",
                    "Copy was updated; reload before retrying");
        }
    }

    public static LibraryCatalogException copyNotFound(String barcode) {
        return new LibraryCatalogException(HttpStatus.NOT_FOUND, "COPY_NOT_FOUND",
                "Book copy not found: " + barcode);
    }

    private static LibraryCatalogException stateConflict(String message) {
        return new LibraryCatalogException(HttpStatus.CONFLICT, "COPY_STATE_CONFLICT", message);
    }
}
