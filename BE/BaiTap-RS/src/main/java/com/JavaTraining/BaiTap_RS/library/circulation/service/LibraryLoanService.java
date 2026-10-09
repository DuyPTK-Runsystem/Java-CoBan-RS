package com.JavaTraining.BaiTap_RS.library.circulation.service;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqBorrowCopiesDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqReturnCopiesDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryBorrowResultDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryLoanDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryReturnResultDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LoanStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import com.JavaTraining.BaiTap_RS.library.security.LibraryAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryLoanService {

    private final LibraryLoanBorrowService borrowService;
    private final LibraryLoanReturnService returnService;
    private final LibraryLoanRenewalService renewalService;
    private final LibraryLoanRepository loanRepository;
    private final LibraryPatronRepository patronRepository;
    private final LibraryCirculationMapper mapper;
    private final LibraryAccessPolicy accessPolicy;

    public LibraryBorrowResultDTO borrow(ReqBorrowCopiesDTO request) {
        return borrowService.borrow(request);
    }

    public LibraryReturnResultDTO returnCopies(ReqReturnCopiesDTO request) {
        return returnService.returnCopies(request);
    }

    public LibraryLoanDTO renew(Long loanId) {
        return renewalService.renew(loanId);
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<LibraryLoanDTO> page(Long patronId, LoanStatus status, int page, int pageSize) {
        Long self = patronRepository.findByUserId(AuditContext.currentUserId()).map(LibraryPatron::getId).orElse(null);
        boolean manager = accessPolicy.isManager();
        Long requestedPatron = requestedPatron(patronId, self, manager);
        Page<LibraryLoan> loans = manager
                ? loanRepository.pageForStaff(requestedPatron, status, PageRequest.of(page, pageSize))
                : loanRepository.pageForPatron(requestedPatron, status, PageRequest.of(page, pageSize));
        return new ResultPaginationDTO<>(new ResultPaginationDTO.Meta(page, pageSize, loans.getTotalPages(),
                loans.getTotalElements()), loans.getContent().stream().map(mapper::loan).toList());
    }

    private Long requestedPatron(Long patronId, Long self, boolean manager) {
        if (manager) {
            return patronId;
        }
        if (patronId != null && !patronId.equals(self)) {
            throw new LibraryCirculationException(HttpStatus.FORBIDDEN, "LIBRARY_RESOURCE_FORBIDDEN",
                    "Không có quyền xem lịch sử bạn đọc");
        }
        if (self == null) {
            throw new LibraryCirculationException(HttpStatus.NOT_FOUND, "PATRON_NOT_FOUND",
                    "Không tìm thấy hồ sơ bạn đọc");
        }
        return self;
    }
}
