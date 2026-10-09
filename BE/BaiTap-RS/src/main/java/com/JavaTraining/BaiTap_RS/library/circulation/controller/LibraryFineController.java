package com.JavaTraining.BaiTap_RS.library.circulation.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqPaymentDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqReasonDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryFineDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.FineStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.service.LibraryFineService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/fines")
@RequiredArgsConstructor
public class LibraryFineController {

    private final LibraryFineService fineService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @ApiMessage("Danh sách khoản phạt thư viện")
    public ResultPaginationDTO<LibraryFineDTO> page(
            @RequestParam(required = false) @Positive Long patronId,
            @RequestParam(required = false) FineStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        return fineService.page(patronId, status, page, pageSize);
    }

    @GetMapping("/{fineId}")
    @PreAuthorize("isAuthenticated()")
    @ApiMessage("Chi tiết khoản phạt")
    public LibraryFineDTO get(@PathVariable @Positive Long fineId) {
        return fineService.getForCurrentUser(fineId);
    }

    @PostMapping("/{fineId}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @ApiMessage("Ghi nhận thanh toán khoản phạt")
    public LibraryFineDTO pay(@PathVariable @Positive Long fineId,
            @Valid @RequestBody ReqPaymentDTO request) {
        return fineService.pay(fineId, request.reference());
    }

    @PostMapping("/{fineId}/waive")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiMessage("Miễn khoản phạt")
    public LibraryFineDTO waive(@PathVariable @Positive Long fineId,
            @Valid @RequestBody ReqReasonDTO request) {
        return fineService.waive(fineId, request.reason());
    }
}
