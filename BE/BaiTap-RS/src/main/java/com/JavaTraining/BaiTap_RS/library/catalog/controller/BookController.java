package com.JavaTraining.BaiTap_RS.library.catalog.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqCreateBookDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqUpdateBookDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookDetailDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookSummaryDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.service.BookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Validated
@RequestMapping("/api/v2/books")
@RequiredArgsConstructor
public class BookController {

    private static final String BOOK_ID_PATH = "/{id}";

    private final BookService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @ApiMessage("Lấy danh sách đầu sách")
    public ResultPaginationDTO<ResBookSummaryDTO> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @Min(1) @Max(9999) Integer publishedYear,
            @RequestParam(required = false) String availability,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam MultiValueMap<String, String> parameters) {
        return service.search(keyword, category, publishedYear, parseAvailability(availability), page, size,
                parameters.getOrDefault("sort", List.of()));
    }

    private Boolean parseAvailability(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if ("AVAILABLE".equalsIgnoreCase(value)) {
            return true;
        }
        if ("UNAVAILABLE".equalsIgnoreCase(value)) {
            return false;
        }
        throw new com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryCatalogException(
                HttpStatus.BAD_REQUEST, "INVALID_CATALOG_QUERY",
                "availability must be AVAILABLE or UNAVAILABLE");
    }

    @GetMapping(BOOK_ID_PATH)
    @PreAuthorize("isAuthenticated()")
    @ApiMessage("Xem chi tiết đầu sách")
    public ResBookDetailDTO get(@PathVariable @Positive Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @ApiMessage("Tạo đầu sách")
    public ResponseEntity<ResBookDetailDTO> create(@Valid @RequestBody ReqCreateBookDTO request) {
        ResBookDetailDTO created = service.create(request);
        java.net.URI location = ServletUriComponentsBuilder.fromCurrentRequest().path(BOOK_ID_PATH)
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping(BOOK_ID_PATH)
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @ApiMessage("Cập nhật đầu sách")
    public ResBookDetailDTO update(@PathVariable @Positive Long id,
            @Valid @RequestBody ReqUpdateBookDTO request) {
        return service.update(id, request);
    }

    @DeleteMapping(BOOK_ID_PATH)
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @ApiMessage("Lưu trữ đầu sách")
    public ResponseEntity<Void> archive(@PathVariable @Positive Long id,
            @RequestParam @Min(0) Long expectedVersion) {
        service.archive(id, expectedVersion);
        return ResponseEntity.noContent().build();
    }
}
