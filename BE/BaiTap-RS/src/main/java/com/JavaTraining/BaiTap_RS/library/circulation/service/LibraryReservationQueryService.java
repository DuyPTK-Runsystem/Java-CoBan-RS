package com.JavaTraining.BaiTap_RS.library.circulation.service;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryReservationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryReservation;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryReservationRepository;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import com.JavaTraining.BaiTap_RS.library.security.LibraryAccessPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LibraryReservationQueryService {
    private final LibraryReservationRepository reservationRepository;
    private final LibraryPatronRepository patronRepository;
    private final LibraryCirculationMapper mapper;
    private final LibraryAccessPolicy accessPolicy;

    @Transactional(readOnly = true)
    public ResultPaginationDTO<LibraryReservationDTO> page(Long bookId, Long patronId, ReservationStatus status,
            int page, int pageSize) {
        Long self = patronRepository.findByUserId(AuditContext.currentUserId()).map(LibraryPatron::getId).orElse(null);
        boolean manager = accessPolicy.isManager();
        Long requestedPatron = resolveRequestedPatron(patronId, self, manager);
        Page<LibraryReservation> items = manager
                ? reservationRepository.pageForStaff(bookId, requestedPatron, status, PageRequest.of(page, pageSize))
                : reservationRepository.pageForPatron(bookId, requestedPatron, status, PageRequest.of(page, pageSize));
        return new ResultPaginationDTO<>(new ResultPaginationDTO.Meta(page, pageSize, items.getTotalPages(),
                items.getTotalElements()), items.getContent().stream().map(mapper::reservation).toList());
    }

    private Long resolveRequestedPatron(Long requestedPatron, Long self, boolean manager) {
        if (manager) {
            return requestedPatron;
        }
        if (requestedPatron != null && !requestedPatron.equals(self)) {
            throw error(HttpStatus.FORBIDDEN, "LIBRARY_RESOURCE_FORBIDDEN", "Không có quyền xem yêu cầu đặt giữ");
        }
        if (self == null) {
            throw error(HttpStatus.NOT_FOUND, "PATRON_NOT_FOUND", "Không tìm thấy hồ sơ bạn đọc");
        }
        return self;
    }

    private LibraryCirculationException error(HttpStatus status, String code, String message) {
        return new LibraryCirculationException(status, code, message);
    }
}
