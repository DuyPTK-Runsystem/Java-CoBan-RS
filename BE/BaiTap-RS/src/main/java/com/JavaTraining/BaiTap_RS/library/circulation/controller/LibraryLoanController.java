package com.JavaTraining.BaiTap_RS.library.circulation.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqBorrowCopiesDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqReturnCopiesDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryBorrowResultDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryLoanDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryReturnResultDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LoanStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.service.LibraryLoanService;
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
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class LibraryLoanController {

    private final LibraryLoanService loanService;

    @PostMapping("/loans")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @ApiMessage("Tạo khoản mượn")
    public LibraryBorrowResultDTO borrow(@Valid @RequestBody ReqBorrowCopiesDTO request) {
        return loanService.borrow(request);
    }

    @GetMapping("/loans")
    @PreAuthorize("isAuthenticated()")
    @ApiMessage("Lịch sử mượn sách")
    public ResultPaginationDTO<LibraryLoanDTO> page(
            @RequestParam(required = false) @Positive Long patronId,
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        return loanService.page(patronId, status, page, pageSize);
    }

    @PostMapping("/loans/{loanId}/renew")
    @PreAuthorize("isAuthenticated()")
    @ApiMessage("Gia hạn khoản mượn")
    public LibraryLoanDTO renew(@PathVariable @Positive Long loanId) {
        return loanService.renew(loanId);
    }

    @PostMapping("/returns")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @ApiMessage("Nhận trả sách")
    public LibraryReturnResultDTO returnCopies(@Valid @RequestBody ReqReturnCopiesDTO request) {
        return loanService.returnCopies(request);
    }
}
