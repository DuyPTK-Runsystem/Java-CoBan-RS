package com.JavaTraining.BaiTap_RS.library.card.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqIssueLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqReissueLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqRevokeLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqVerifyLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.response.ResLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.response.ResLibraryCardVerificationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LibraryCardService {

    private final LibraryCardCommandService commandService;
    private final LibraryCardQueryService queryService;
    private final LibraryCardVerificationService verificationService;

    public ResLibraryCardDTO issue(ReqIssueLibraryCardDTO request) {
        return commandService.issue(request);
    }

    public ResLibraryCardDTO reissue(String cardNo, ReqReissueLibraryCardDTO request) {
        return commandService.reissue(cardNo, request);
    }

    public ResLibraryCardDTO revoke(String cardNo, ReqRevokeLibraryCardDTO request) {
        return commandService.revoke(cardNo, request);
    }

    public ResLibraryCardDTO getMe() {
        return queryService.getMe();
    }

    public List<ResLibraryCardDTO> historyForPatron(Long patronId) {
        return queryService.historyForPatron(patronId);
    }

    public List<ResLibraryCardDTO> myHistory() {
        return queryService.myHistory();
    }

    public byte[] qrPng(String cardNo) {
        return queryService.qrPng(cardNo);
    }

    public ResLibraryCardVerificationDTO verify(ReqVerifyLibraryCardDTO request) {
        return verificationService.verify(request);
    }
}
