package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.util.List;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryFineDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.FineStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryFine;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryFineRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import com.JavaTraining.BaiTap_RS.library.security.LibraryAccessPolicy;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LibraryFineQueryService {
    private static final String FINE_NOT_FOUND_CODE = "FINE_NOT_FOUND";
    private static final String FINE_NOT_FOUND_MESSAGE = "Không tìm thấy khoản phạt";
    private final LibraryFineRepository fineRepository;
    private final LibraryLoanRepository loanRepository;
    private final LibraryCirculationMapper mapper;
    private final LibraryPatronRepository patronRepository;
    private final LibraryAccessPolicy accessPolicy;
    @Transactional(readOnly = true)
    public LibraryFineDTO get(Long fineId, Long patronId) {
        LibraryFine fine = fineRepository.findById(fineId).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, FINE_NOT_FOUND_CODE, FINE_NOT_FOUND_MESSAGE));
        LibraryLoan loan = findLoan(fine.getLoanId());
        if (patronId != null && !patronId.equals(loan.getPatronId())) {
            throw error(HttpStatus.FORBIDDEN, "LIBRARY_RESOURCE_FORBIDDEN", "Không có quyền xem khoản phạt");
        }
        return mapper.fine(fine);
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<LibraryFineDTO> page(Long patronId, FineStatus status, int page, int pageSize) {
        Long selfId = AuditContext.currentUserId();
        Long selfPatron = patronRepository.findByUserId(selfId).map(LibraryPatron::getId).orElse(null);
        boolean manager = accessPolicy.isManager();
        Long queryPatron = resolvePagePatron(patronId, selfPatron, manager);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<LibraryFine> fines = manager
                ? fineRepository.pageForStaff(queryPatron, status, pageable)
                : fineRepository.pageForPatron(queryPatron, status, pageable);
        List<LibraryFineDTO> result = fines.getContent().stream().map(mapper::fine).toList();
        return new ResultPaginationDTO<>(new ResultPaginationDTO.Meta(
                page, pageSize, fines.getTotalPages(), fines.getTotalElements()), result);
    }

    @Transactional(readOnly = true)
    public LibraryFineDTO getForCurrentUser(Long fineId) {
        LibraryFine fine = fineRepository.findById(fineId).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, FINE_NOT_FOUND_CODE, FINE_NOT_FOUND_MESSAGE));
        LibraryLoan loan = findLoan(fine.getLoanId());
        Long selfPatron = patronRepository.findByUserId(AuditContext.currentUserId())
                .map(LibraryPatron::getId).orElse(null);
        if (!accessPolicy.isManager()
                && !loan.getPatronId().equals(selfPatron)) {
            throw error(HttpStatus.FORBIDDEN, "LIBRARY_RESOURCE_FORBIDDEN", "Không có quyền xem khoản phạt");
        }
        return mapper.fine(fine);
    }
    private LibraryLoan findLoan(Long loanId) {
        return loanRepository.findById(loanId).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "ACTIVE_LOAN_NOT_FOUND", "Không tìm thấy khoản mượn"));
    }

    private Long resolvePagePatron(Long requestedPatron, Long selfPatron, boolean manager) {
        if (manager) {
            return requestedPatron;
        }
        if (requestedPatron != null && !requestedPatron.equals(selfPatron)) {
            throw error(HttpStatus.FORBIDDEN, "LIBRARY_RESOURCE_FORBIDDEN", "Không có quyền xem khoản phạt");
        }
        if (selfPatron == null) {
            throw error(HttpStatus.NOT_FOUND, "PATRON_NOT_FOUND", "Không tìm thấy hồ sơ bạn đọc");
        }
        return selfPatron;
    }

    private LibraryCirculationException error(HttpStatus status, String code, String message) {
        return new LibraryCirculationException(status, code, message);
    }
}
