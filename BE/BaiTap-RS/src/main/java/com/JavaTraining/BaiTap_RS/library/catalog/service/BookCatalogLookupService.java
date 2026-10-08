package com.JavaTraining.BaiTap_RS.library.catalog.service;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookSummaryDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryCatalogException;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookCatalogLookupService {

    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;

    @Transactional(readOnly = true)
    public Book activeBook(Long id) {
        return bookRepository.findById(id).filter(book -> !book.isArchived())
                .orElseThrow(() -> bookNotFound(id));
    }

    @Transactional
    public Book activeBookForUpdate(Long id) {
        return bookRepository.findByIdForUpdate(id).filter(book -> !book.isArchived())
                .orElseThrow(() -> bookNotFound(id));
    }

    @Transactional
    public Book bookForUpdate(Long id) {
        return bookRepository.findByIdForUpdate(id)
                .orElseThrow(() -> bookNotFound(id));
    }

    @Transactional(readOnly = true)
    public ResBookSummaryDTO summary(Book book) {
        return BookCatalogMapper.summary(book, copyRepository.countByBookId(book.getId()),
                copyRepository.countByBookIdAndStatusAndReferenceOnlyFalse(book.getId(), BookCopyStatus.AVAILABLE));
    }

    private LibraryCatalogException bookNotFound(Long id) {
        return new LibraryCatalogException(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Book not found: " + id);
    }
}
