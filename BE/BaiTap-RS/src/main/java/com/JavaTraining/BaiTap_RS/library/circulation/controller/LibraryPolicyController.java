package com.JavaTraining.BaiTap_RS.library.circulation.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqLibraryPolicyDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryPolicyDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.service.LibraryPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/library/policies/circulation")
@RequiredArgsConstructor
public class LibraryPolicyController {

    private final LibraryPolicyService policyService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @ApiMessage("Chính sách lưu thông thư viện hiện tại")
    public LibraryPolicyDTO current() {
        return policyService.current();
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @ApiMessage("Cập nhật chính sách lưu thông thư viện")
    public LibraryPolicyDTO update(@Valid @RequestBody ReqLibraryPolicyDTO request) {
        return policyService.update(request);
    }
}
