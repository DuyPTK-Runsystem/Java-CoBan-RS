package com.JavaTraining.BaiTap_RS.library.patron.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.requests.ReqActivateLibraryPatronDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.requests.ReqUpdateLibraryPatronStatusDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.response.ResLibraryActivationCandidateDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.response.ResLibraryPatronDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronStatus;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import com.JavaTraining.BaiTap_RS.library.patron.service.LibraryPatronService;
import com.JavaTraining.BaiTap_RS.library.security.LibraryAccessPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/library-patrons")
@RequiredArgsConstructor
public class LibraryPatronController {

    private final LibraryPatronService service;

    @GetMapping
    @PreAuthorize(LibraryAccessPolicy.MANAGER_CHECK)
    @ApiMessage("Lấy danh sách bạn đọc")
    public ResultPaginationDTO<ResLibraryPatronDTO> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sort) {
        LibraryPatronStatus parsedStatus = parseStatus(status);
        SortSpec sortSpec = parseSort(sort);
        return service.search(keyword, parsedStatus, page, size, sortSpec.field(), sortSpec.direction());
    }

    @GetMapping("/activation-candidates")
    @PreAuthorize(LibraryAccessPolicy.MANAGER_CHECK)
    @ApiMessage("Lấy tài khoản có thể kích hoạt thành bạn đọc")
    public ResultPaginationDTO<ResLibraryActivationCandidateDTO> activationCandidates(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.activationCandidates(keyword, page, size);
    }

    @PostMapping
    @PreAuthorize(LibraryAccessPolicy.MANAGER_CHECK)
    @ApiMessage("Kích hoạt hồ sơ bạn đọc")
    public ResponseEntity<ResLibraryPatronDTO> activate(@Valid @RequestBody ReqActivateLibraryPatronDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.activate(request));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @ApiMessage("Xem hồ sơ bạn đọc của tôi")
    public ResLibraryPatronDTO getMe() {
        return service.getMe();
    }

    @GetMapping("/{patronId}")
    @PreAuthorize(LibraryAccessPolicy.MANAGER_CHECK)
    @ApiMessage("Xem chi tiết bạn đọc")
    public ResLibraryPatronDTO get(@PathVariable @Positive Long patronId) {
        return service.get(patronId);
    }

    @PatchMapping("/{patronId}/status")
    @PreAuthorize(LibraryAccessPolicy.MANAGER_CHECK)
    @ApiMessage("Thay đổi trạng thái bạn đọc")
    public ResLibraryPatronDTO updateStatus(@PathVariable @Positive Long patronId,
            @Valid @RequestBody ReqUpdateLibraryPatronStatusDTO request) {
        return service.updateStatus(patronId, request);
    }

    private LibraryPatronStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return LibraryPatronStatus.valueOf(status.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new LibraryPatronException(HttpStatus.BAD_REQUEST, "INVALID_LIBRARY_QUERY",
                    "Trạng thái bạn đọc không hợp lệ", exception);
        }
    }

    private SortSpec parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new SortSpec(null, Sort.Direction.ASC);
        }
        String[] tokens = sort.split(",", -1);
        if (tokens.length > 2) {
            throw new LibraryPatronException(HttpStatus.BAD_REQUEST, "INVALID_LIBRARY_QUERY",
                    "Trường sắp xếp không hợp lệ");
        }
        Sort.Direction direction = Sort.Direction.ASC;
        if (tokens.length == 2) {
            try {
                direction = Sort.Direction.fromString(tokens[1]);
            } catch (IllegalArgumentException exception) {
                throw new LibraryPatronException(HttpStatus.BAD_REQUEST, "INVALID_LIBRARY_QUERY",
                        "Chiều sắp xếp không hợp lệ", exception);
            }
        }
        return new SortSpec(tokens[0], direction);
    }

    private record SortSpec(String field, Sort.Direction direction) { }
}
