package com.JavaTraining.BaiTap_RS.library.circulation.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.library.circulation.batch.LibraryBatchService;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqOverdueFineRunDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryBatchRunDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/library/batch-jobs")
@RequiredArgsConstructor
public class LibraryBatchController {

    private final LibraryBatchService batchService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @ApiMessage("Lịch sử batch thư viện")
    public List<LibraryBatchRunDTO> history() {
        return batchService.history();
    }

    @PostMapping("/overdue-fine")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @ApiMessage("Chạy tính phí trễ hạn")
    public LibraryBatchRunDTO run(@Valid @RequestBody ReqOverdueFineRunDTO request) {
        return batchService.run(request.runDate());
    }
}
