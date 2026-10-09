package com.JavaTraining.BaiTap_RS.library.card.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqIssueLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqReissueLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqRevokeLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqVerifyLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.response.ResLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.response.ResLibraryCardVerificationDTO;
import com.JavaTraining.BaiTap_RS.library.card.service.LibraryCardService;
import com.JavaTraining.BaiTap_RS.library.security.LibraryAccessPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/library-cards")
@RequiredArgsConstructor
public class LibraryCardController {

    private final LibraryCardService service;

    @PostMapping
    @PreAuthorize(LibraryAccessPolicy.MANAGER_CHECK)
    @ApiMessage("Phát hành thẻ thư viện")
    public ResponseEntity<ResLibraryCardDTO> issue(@Valid @RequestBody ReqIssueLibraryCardDTO request) {
        return ResponseEntity.status(201).body(service.issue(request));
    }

    @PostMapping("/{cardNo}/reissue")
    @PreAuthorize(LibraryAccessPolicy.MANAGER_CHECK)
    @ApiMessage("Cấp lại thẻ thư viện")
    public ResLibraryCardDTO reissue(@PathVariable @NotBlank String cardNo,
            @Valid @RequestBody ReqReissueLibraryCardDTO request) {
        return service.reissue(cardNo, request);
    }

    @PostMapping("/{cardNo}/revoke")
    @PreAuthorize(LibraryAccessPolicy.MANAGER_CHECK)
    @ApiMessage("Thu hồi thẻ thư viện")
    public ResLibraryCardDTO revoke(@PathVariable @NotBlank String cardNo,
            @Valid @RequestBody ReqRevokeLibraryCardDTO request) {
        return service.revoke(cardNo, request);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @ApiMessage("Xem thẻ thư viện của tôi")
    public ResLibraryCardDTO getMe() {
        return service.getMe();
    }

    @GetMapping("/me/history")
    @PreAuthorize("isAuthenticated()")
    @ApiMessage("Xem lịch sử thẻ của tôi")
    public List<ResLibraryCardDTO> myHistory() {
        return service.myHistory();
    }

    @GetMapping("/{cardNo}/qr.png")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> qrPng(@PathVariable @NotBlank String cardNo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setCacheControl(CacheControl.noStore());
        return ResponseEntity.ok().headers(headers).body(service.qrPng(cardNo));
    }

    @PostMapping("/verify")
    @PreAuthorize(LibraryAccessPolicy.MANAGER_CHECK)
    @ApiMessage("Xác thực thẻ thư viện")
    public ResLibraryCardVerificationDTO verify(@Valid @RequestBody ReqVerifyLibraryCardDTO request) {
        return service.verify(request);
    }

}
