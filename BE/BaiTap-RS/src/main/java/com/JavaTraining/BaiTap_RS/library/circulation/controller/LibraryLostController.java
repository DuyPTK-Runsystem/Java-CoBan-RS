package com.JavaTraining.BaiTap_RS.library.circulation.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqReasonDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryLostResultDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.service.LibraryLostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/book-copies")
@RequiredArgsConstructor
public class LibraryLostController {

    private final LibraryLostService lostService;

    @PostMapping("/{barcode}/lost")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @ApiMessage("Đánh dấu sách bị mất")
    public LibraryLostResultDTO markLost(@PathVariable @NotBlank @Size(max = 64) String barcode,
            @Valid @RequestBody ReqReasonDTO request) {
        return lostService.markLost(barcode, request.reason());
    }
}
