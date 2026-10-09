package com.JavaTraining.BaiTap_RS.library.patron.service;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.requests.ReqActivateLibraryPatronDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.requests.ReqUpdateLibraryPatronStatusDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.response.ResLibraryActivationCandidateDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.response.ResLibraryPatronDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronStatus;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import com.JavaTraining.BaiTap_RS.library.security.LibraryAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryPatronService {

    private final LibraryPatronRepository patronRepository;
    private final LibraryPatronReadModelService readModelService;
    private final LibraryPatronActivationService activationService;
    private final LibraryPatronStatusService statusService;
    private final LibraryAccessPolicy accessPolicy;
    private final LibraryPatronPageableResolver pageableResolver;

    @Transactional(readOnly = true)
    public ResultPaginationDTO<ResLibraryPatronDTO> search(String keyword, LibraryPatronStatus status,
            int page, int size, String sortField, Sort.Direction direction) {
        Pageable pageable = pageableResolver.resolve(page, size, sortField, direction);
        Page<LibraryPatron> result = patronRepository.search(normalizeKeyword(keyword), status, pageable);
        return new ResultPaginationDTO<>(new ResultPaginationDTO.Meta(result.getNumber(), result.getSize(),
                result.getTotalPages(), result.getTotalElements()), readModelService.patrons(result.getContent()));
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<ResLibraryActivationCandidateDTO> activationCandidates(
            String keyword, int page, int size) {
        return activationService.activationCandidates(keyword, page, size);
    }

    @Transactional
    public ResLibraryPatronDTO activate(ReqActivateLibraryPatronDTO request) {
        return activationService.activate(request);
    }

    @Transactional(readOnly = true)
    public ResLibraryPatronDTO get(Long patronId) {
        LibraryPatron patron = patronRepository.findById(patronId).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "PATRON_NOT_FOUND", "Không tìm thấy hồ sơ bạn đọc"));
        return readModelService.patron(patron);
    }

    @Transactional(readOnly = true)
    public ResLibraryPatronDTO getMe() {
        Long userId = accessPolicy.currentUserId();
        if (userId == null) {
            throw error(HttpStatus.UNAUTHORIZED, "LIBRARY_RESOURCE_FORBIDDEN", "Chưa xác thực tài khoản");
        }
        LibraryPatron patron = patronRepository.findByUserId(userId).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "PATRON_NOT_FOUND", "Không tìm thấy hồ sơ bạn đọc"));
        return readModelService.patron(patron);
    }

    @Transactional
    public ResLibraryPatronDTO updateStatus(Long patronId, ReqUpdateLibraryPatronStatusDTO request) {
        return statusService.updateStatus(patronId, request);
    }

    private LibraryPatronException error(HttpStatus status, String code, String message) {
        return new LibraryPatronException(status, code, message);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}
