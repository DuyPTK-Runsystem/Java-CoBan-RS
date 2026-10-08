package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqCreateBookDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqUpdateBookDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookDetailDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookSummaryDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryCatalogException;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

    private static final List<BookCopyStatus> ACTIVE_COPY_STATES = List.of(
            BookCopyStatus.ON_LOAN, BookCopyStatus.RESERVED);

    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final BookCatalogLookupService lookupService;
    private final BookSortResolver sortResolver;
    private final LibraryCatalogAuditService auditService;

    @Transactional(readOnly = true)
    public ResultPaginationDTO<ResBookSummaryDTO> search(String keyword, String category,
            Integer publishedYear, Boolean available, int pageNumber, int size, List<String> sort) {
        Pageable pageable = sortResolver.resolve(pageNumber, size, sort);
        Page<Book> page = bookRepository.findAll(
                BookSpecifications.catalog(keyword, category, publishedYear, available), pageable);
        List<Long> ids = page.getContent().stream().map(Book::getId).toList();
        Map<Long, Long> totalCounts = ids.isEmpty() ? Map.of()
                : BookCatalogMapper.counts(copyRepository.countCopiesByBookIds(ids));
        Map<Long, Long> availableCounts = ids.isEmpty() ? Map.of()
                : BookCatalogMapper.counts(copyRepository.countAvailableByBookIds(ids, BookCopyStatus.AVAILABLE));
        List<ResBookSummaryDTO> result = page.getContent().stream()
                .map(book -> BookCatalogMapper.summary(book, totalCounts.getOrDefault(book.getId(), 0L),
                        availableCounts.getOrDefault(book.getId(), 0L))).toList();
        return new ResultPaginationDTO<>(new ResultPaginationDTO.Meta(page.getNumber(), page.getSize(),
                page.getTotalPages(), page.getTotalElements()), result);
    }

    @Transactional(readOnly = true)
    public ResBookDetailDTO get(Long id) {
        return detail(lookupService.activeBook(id));
    }

    @Transactional
    public ResBookDetailDTO create(ReqCreateBookDTO request) {
        String isbn = BookValidation.normalizeIsbn(request.isbn());
        ensureUniqueIsbn(isbn, null);
        Book book = new Book(isbn, request.title().trim(), request.author().trim(),
                BookValidation.normalizeText(request.publisher()), request.publishedYear(),
                BookValidation.normalizeText(request.category()), request.listPrice(),
                BookValidation.validateCoverUrl(request.coverUrl()));
        Book saved = bookRepository.saveAndFlush(book);
        auditService.record("BOOK_CREATED", "book", saved.getId(), null, BookCatalogMapper.audit(saved));
        return detail(saved);
    }

    @Transactional
    public ResBookDetailDTO update(Long id, ReqUpdateBookDTO request) {
        Book book = lookupService.activeBookForUpdate(id);
        requireVersion(book.getVersion(), request.expectedVersion());
        Map<String, Object> before = BookCatalogMapper.audit(book);
        String isbn = BookValidation.normalizeIsbn(request.isbn());
        ensureUniqueIsbn(isbn, id);
        book.setIsbn(isbn);
        book.setTitle(request.title().trim());
        book.setAuthor(request.author().trim());
        book.setPublisher(BookValidation.normalizeText(request.publisher()));
        book.setPublishedYear(request.publishedYear());
        book.setCategory(BookValidation.normalizeText(request.category()));
        book.setListPrice(request.listPrice());
        book.setCoverUrl(BookValidation.validateCoverUrl(request.coverUrl()));
        bookRepository.flush();
        auditService.record("BOOK_UPDATED", "book", id, before, BookCatalogMapper.audit(book));
        return detail(book);
    }

    @Transactional
    public void archive(Long id, Long expectedVersion) {
        Book book = lookupService.activeBookForUpdate(id);
        requireVersion(book.getVersion(), expectedVersion);
        if (copyRepository.existsByBookIdAndStatusIn(id, ACTIVE_COPY_STATES)) {
            throw new LibraryCatalogException(HttpStatus.CONFLICT, "BOOK_HAS_ACTIVE_CIRCULATION",
                    "Book has copies on loan or reserved");
        }
        Map<String, Object> before = BookCatalogMapper.audit(book);
        book.setArchivedAt(java.time.LocalDateTime.now());
        bookRepository.flush();
        auditService.record("BOOK_ARCHIVED", "book", id, before, BookCatalogMapper.audit(book));
    }

    private ResBookDetailDTO detail(Book book) {
        return BookCatalogMapper.detail(book, lookupService.summary(book));
    }

    private void ensureUniqueIsbn(String isbn, Long id) {
        if (isbn != null && (id == null ? bookRepository.existsByIsbn(isbn)
                : bookRepository.existsByIsbnAndIdNot(isbn, id))) {
            throw new LibraryCatalogException(HttpStatus.CONFLICT, "DUPLICATE_ISBN", "ISBN already exists");
        }
    }

    private void requireVersion(Long currentVersion, Long expectedVersion) {
        if (!currentVersion.equals(expectedVersion)) {
            throw new LibraryCatalogException(HttpStatus.CONFLICT, "VERSION_CONFLICT",
                    "Book was updated; reload before retrying");
        }
    }

}
