package com.JavaTraining.BaiTap_RS.library.circulation.service;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqReservationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryReservationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LibraryReservationService {

    private final LibraryReservationCommandService commandService;
    private final LibraryReservationQueryService queryService;

    public LibraryReservationDTO create(ReqReservationDTO request) {
        return commandService.create(request);
    }

    public LibraryReservationDTO cancel(Long reservationId) {
        return commandService.cancel(reservationId);
    }

    public ResultPaginationDTO<LibraryReservationDTO> page(Long patronId, ReservationStatus status,
            int page, int pageSize) {
        return queryService.page(patronId, status, page, pageSize);
    }

    public int expireReadyReservations(Long actorUserId) {
        return commandService.expireReadyReservations(actorUserId);
    }
}
