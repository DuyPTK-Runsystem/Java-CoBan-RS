package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqCreateBookCopiesDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookCopyBatchDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookCopyDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyBatchRequest;
import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryCatalogException;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyBatchRequestRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookCopyBatchService {

    private final BookCatalogLookupService bookLookup;
    private final BookCopyRepository copyRepository;
    private final BookCopyBatchRequestRepository batchRepository;
    private final LibraryCatalogAuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ResBookCopyBatchDTO create(Long bookId, ReqCreateBookCopiesDTO request, String idempotencyKey) {
        validateQuantity(request);
        validateIdempotencyKey(idempotencyKey);
        Book book = bookLookup.activeBookForUpdate(bookId);
        Long actorId = AuditContext.currentUserId();
        if (actorId == null) {
            throw new LibraryCatalogException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                    "Authentication is required");
        }
        String fingerprint = fingerprint(request);
        Optional<BookCopyBatchRequest> previous = batchRepository
                .findByActorUserIdAndBookIdAndIdempotencyKey(actorId, bookId, idempotencyKey);
        if (previous.isPresent()) {
            if (!previous.get().getPayloadFingerprint().equals(fingerprint)) {
                throw new LibraryCatalogException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT",
                        "Idempotency-Key was already used with a different payload");
            }
            return deserialize(previous.get().getResponseJson());
        }
        List<BookCopy> copies = new ArrayList<>();
        String shelf = BookValidation.normalizeText(request.shelfLocation());
        for (int index = 0; index < request.quantity(); index++) {
            copies.add(new BookCopy(book, "TMP-" + UUID.randomUUID(), shelf, request.referenceOnly()));
        }
        copies = copyRepository.saveAllAndFlush(copies);
        for (BookCopy copy : copies) {
            copy.setBarcode(BookBarcodeGenerator.barcodeFor(copy.getId()));
        }
        copyRepository.flush();
        List<ResBookCopyDTO> responses = copies.stream().map(BookCopyMapper::toResponse).toList();
        ResBookCopyBatchDTO response = new ResBookCopyBatchDTO(bookId, responses.size(), responses);
        batchRepository.saveAndFlush(new BookCopyBatchRequest(actorId, bookId, idempotencyKey,
                fingerprint, serialize(response)));
        List<Long> copyIds = responses.stream().map(ResBookCopyDTO::id).toList();
        auditService.record("BOOK_COPIES_CREATED", "book", bookId, null,
                Map.of("createdCount", responses.size(), "copyIds", copyIds));
        return response;
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 128) {
            throw new LibraryCatalogException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key is required and must be at most 128 characters");
        }
    }

    private void validateQuantity(ReqCreateBookCopiesDTO request) {
        if (request == null || request.quantity() == null || request.quantity() < 1 || request.quantity() > 100) {
            throw new LibraryCatalogException(HttpStatus.BAD_REQUEST, "INVALID_COPY_QUANTITY",
                    "Copy quantity must be between 1 and 100");
        }
    }

    private String fingerprint(ReqCreateBookCopiesDTO request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("quantity", request.quantity());
        body.put("shelfLocation", BookValidation.normalizeText(request.shelfLocation()));
        body.put("referenceOnly", request.referenceOnly());
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to fingerprint batch request", exception);
        }
    }

    private String serialize(ResBookCopyBatchDTO response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store batch result", exception);
        }
    }

    private ResBookCopyBatchDTO deserialize(String response) {
        try {
            return objectMapper.readValue(response, ResBookCopyBatchDTO.class);
        } catch (JsonProcessingException exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read stored batch result", exception);
        }
    }
}
