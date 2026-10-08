package com.JavaTraining.BaiTap_RS.library.catalog.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqUpdateBookCopyDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookCopyDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookCopyLookupDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.catalog.service.BookCopyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class BookCopyController {

    private final BookCopyService service;

    @GetMapping("/books/{bookId}/copies")
    @PreAuthorize("isAuthenticated()")
    @ApiMessage("Lấy danh sách bản sao")
    public ResultPaginationDTO<ResBookCopyDTO> list(
            @PathVariable @Positive Long bookId,
            @RequestParam(required = false) BookCopyStatus status,
            @RequestParam(required = false) Boolean referenceOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @jakarta.validation.constraints.Max(100) int size,
            @RequestParam MultiValueMap<String, String> parameters) {
        return service.list(bookId, status, referenceOnly, page, size, parameters.getOrDefault("sort", List.of()));
    }

    @GetMapping("/book-copies/{barcode}")
    @PreAuthorize("isAuthenticated()")
    @ApiMessage("Tra cứu bản sao bằng barcode")
    public ResBookCopyLookupDTO get(@PathVariable @NotBlank String barcode) {
        return service.getByBarcode(barcode);
    }

    @PatchMapping("/book-copies/{barcode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @ApiMessage("Cập nhật bản sao")
    public ResBookCopyDTO update(@PathVariable @NotBlank String barcode,
            @Valid @RequestBody ReqUpdateBookCopyDTO request) {
        return service.update(barcode, request);
    }

    @DeleteMapping("/book-copies/{barcode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @ApiMessage("Rút bản sao khỏi lưu thông")
    public ResponseEntity<Void> withdraw(@PathVariable @NotBlank String barcode,
            @RequestParam @Min(0) Long expectedVersion) {
        service.withdraw(barcode, expectedVersion);
        return ResponseEntity.noContent().build();
    }
}
