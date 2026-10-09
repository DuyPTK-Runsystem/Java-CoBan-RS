package com.JavaTraining.BaiTap_RS.library.circulation.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqReservationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryReservationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.service.LibraryReservationService;
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
@RequestMapping("/api/v2/reservations")
@RequiredArgsConstructor
public class LibraryReservationController {

    private final LibraryReservationService reservationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @ApiMessage("Tạo yêu cầu đặt giữ")
    public LibraryReservationDTO create(@Valid @RequestBody ReqReservationDTO request) {
        return reservationService.create(request);
    }

    @PostMapping("/{reservationId}/cancel")
    @PreAuthorize("isAuthenticated()")
    @ApiMessage("Hủy yêu cầu đặt giữ")
    public LibraryReservationDTO cancel(@PathVariable @Positive Long reservationId) {
        return reservationService.cancel(reservationId);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @ApiMessage("Lịch sử và hàng chờ đặt giữ")
    public ResultPaginationDTO<LibraryReservationDTO> page(
            @RequestParam(required = false) @Positive Long patronId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        return reservationService.page(patronId, status, page, pageSize);
    }
}
