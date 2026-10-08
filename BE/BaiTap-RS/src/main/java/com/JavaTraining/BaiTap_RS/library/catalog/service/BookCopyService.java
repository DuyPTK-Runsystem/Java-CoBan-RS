package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqCreateBookCopiesDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqUpdateBookCopyDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookCopyBatchDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookCopyDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookCopyLookupDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryCatalogException;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookCopyService {

    private final BookCatalogLookupService bookLookup;
    private final BookCopyRepository copyRepository;
    private final BookSortResolver sortResolver;
    private final BookCopyBatchService batchService;
    private final LibraryCatalogAuditService auditService;

    @Transactional(readOnly = true)
    public ResultPaginationDTO<ResBookCopyDTO> list(Long bookId, BookCopyStatus status,
            Boolean referenceOnly, int page, int size, List<String> sort) {
        bookLookup.activeBook(bookId);
        Pageable pageable = sortResolver.resolveCopies(page, size, sort);
        Page<BookCopy> copies = copyRepository.searchByBookId(bookId, status, referenceOnly, pageable);
        return new ResultPaginationDTO<>(new ResultPaginationDTO.Meta(copies.getNumber(), copies.getSize(),
                copies.getTotalPages(), copies.getTotalElements()),
                copies.getContent().stream().map(BookCopyMapper::toResponse).toList());
    }

    public ResBookCopyBatchDTO createBatch(Long bookId, ReqCreateBookCopiesDTO request, String idempotencyKey) {
        return batchService.create(bookId, request, idempotencyKey);
    }

    @Transactional(readOnly = true)
    public ResBookCopyLookupDTO getByBarcode(String barcode) {
        BookCopy copy = copyRepository.findByBarcode(barcode)
                .orElseThrow(() -> new LibraryCatalogException(HttpStatus.NOT_FOUND,
                        "COPY_NOT_FOUND", "Book copy not found"));
        if (copy.getBook().isArchived()) {
            throw new LibraryCatalogException(HttpStatus.CONFLICT, "BOOK_ARCHIVED", "Book is archived");
        }
        return new ResBookCopyLookupDTO(BookCopyMapper.toResponse(copy), bookLookup.summary(copy.getBook()));
    }

    @Transactional
    public ResBookCopyDTO update(String barcode, ReqUpdateBookCopyDTO request) {
        Long bookId = copyRepository.findBookIdByBarcode(barcode)
                .orElseThrow(() -> BookCopyPolicy.copyNotFound(barcode));
        BookCopyPolicy.ensureBookActive(bookLookup.bookForUpdate(bookId));
        BookCopy copy = copyRepository.findByBarcodeForUpdate(barcode)
                .orElseThrow(() -> BookCopyPolicy.copyNotFound(barcode));
        BookCopyPolicy.ensureEditable(copy);
        BookCopyPolicy.requireVersion(copy.getVersion(), request.getExpectedVersion());
        java.util.Map<String, Object> before = BookCopyMapper.auditSnapshot(copy);
        if (request.isShelfLocationProvided()) {
            copy.setShelfLocation(BookValidation.normalizeText(request.getShelfLocation()));
        }
        if (request.isReferenceOnly() != null) {
            copy.setReferenceOnly(request.isReferenceOnly());
        }
        if (request.getStatus() != null) {
            if (!BookCopyPolicy.isAllowedStatusChange(copy.getStatus(), request.getStatus())) {
                throw new LibraryCatalogException(HttpStatus.CONFLICT, "COPY_STATE_CONFLICT",
                        "Unsupported copy status transition");
            }
            copy.setStatus(request.getStatus());
        }
        copyRepository.flush();
        auditService.record("BOOK_COPY_UPDATED", "book_copy", copy.getId(), before,
                BookCopyMapper.auditSnapshot(copy));
        return BookCopyMapper.toResponse(copy);
    }

    @Transactional
    public void withdraw(String barcode, Long expectedVersion) {
        Long bookId = copyRepository.findBookIdByBarcode(barcode)
                .orElseThrow(() -> BookCopyPolicy.copyNotFound(barcode));
        BookCopyPolicy.ensureBookActive(bookLookup.bookForUpdate(bookId));
        BookCopy copy = copyRepository.findByBarcodeForUpdate(barcode)
                .orElseThrow(() -> BookCopyPolicy.copyNotFound(barcode));
        BookCopyPolicy.ensureEditable(copy);
        BookCopyPolicy.requireVersion(copy.getVersion(), expectedVersion);
        if (copy.getStatus() != BookCopyStatus.AVAILABLE && copy.getStatus() != BookCopyStatus.DAMAGED) {
            throw new LibraryCatalogException(HttpStatus.CONFLICT, "COPY_STATE_CONFLICT",
                    "Only AVAILABLE or DAMAGED copies can be withdrawn");
        }
        java.util.Map<String, Object> before = BookCopyMapper.auditSnapshot(copy);
        copy.setStatus(BookCopyStatus.WITHDRAWN);
        copyRepository.flush();
        auditService.record("BOOK_COPY_WITHDRAWN", "book_copy", copy.getId(), before,
                BookCopyMapper.auditSnapshot(copy));
    }

    @Transactional(readOnly = true)
    public byte[] barcodePng(String barcode) {
        getByBarcode(barcode);
        return BookBarcodeGenerator.png(barcode);
    }

}
