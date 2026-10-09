package com.JavaTraining.BaiTap_RS.library.circulation.batch;

import com.JavaTraining.BaiTap_RS.library.circulation.service.LibraryReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LibraryOverdueFineRunService {

    private final LibraryReservationService reservationService;

    public void expireReservations(Long actorUserId) {
        reservationService.expireReadyReservations(actorUserId);
    }
}
