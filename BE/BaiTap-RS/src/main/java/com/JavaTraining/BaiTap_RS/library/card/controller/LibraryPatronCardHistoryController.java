package com.JavaTraining.BaiTap_RS.library.card.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.response.ResLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.service.LibraryCardService;
import com.JavaTraining.BaiTap_RS.library.security.LibraryAccessPolicy;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/library-patrons/{patronId}/cards")
@RequiredArgsConstructor
public class LibraryPatronCardHistoryController {

    private final LibraryCardService service;

    @GetMapping
    @PreAuthorize(LibraryAccessPolicy.MANAGER_CHECK)
    @ApiMessage("Xem lịch sử thẻ bạn đọc")
    public List<ResLibraryCardDTO> history(@PathVariable @Positive Long patronId) {
        return service.historyForPatron(patronId);
    }
}
