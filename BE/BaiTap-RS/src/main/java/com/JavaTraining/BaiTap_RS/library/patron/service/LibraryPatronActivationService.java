package com.JavaTraining.BaiTap_RS.library.patron.service;

import java.time.LocalDateTime;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.requests.ReqActivateLibraryPatronDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.response.ResLibraryActivationCandidateDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.response.ResLibraryPatronDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryPatronActivationService {

    private static final String ENTITY_TYPE = "library_patron";

    private final LibraryPatronRepository patronRepository;
    private final UserRepository userRepository;
    private final LibraryPatronReadModelService readModelService;
    private final LibraryOperationAuditService auditService;
    private final LibraryPatronPageableResolver pageableResolver;

    @Transactional(readOnly = true)
    public ResultPaginationDTO<ResLibraryActivationCandidateDTO> activationCandidates(
            String keyword, int page, int size) {
        Pageable pageable = pageableResolver.candidates(page, size);
        Page<User> candidates = userRepository.findActivationCandidates(normalizeKeyword(keyword), pageable);
        return new ResultPaginationDTO<>(new ResultPaginationDTO.Meta(candidates.getNumber(), candidates.getSize(),
                candidates.getTotalPages(), candidates.getTotalElements()),
                readModelService.candidates(candidates.getContent()));
    }

    @Transactional
    public ResLibraryPatronDTO activate(ReqActivateLibraryPatronDTO request) {
        User user = userRepository.findById(request.userId()).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "PATRON_USER_NOT_FOUND", "Không tìm thấy tài khoản"));
        if (patronRepository.findByUserId(user.getId()).isPresent()) {
            throw error(HttpStatus.CONFLICT, "PATRON_ALREADY_EXISTS", "Tài khoản đã có hồ sơ bạn đọc");
        }
        LocalDateTime now = LocalDateTime.now();
        LibraryPatron patron;
        try {
            patron = patronRepository.saveAndFlush(new LibraryPatron(user.getId(), now));
        } catch (DataIntegrityViolationException exception) {
            throw error(HttpStatus.CONFLICT, "PATRON_ALREADY_EXISTS", "Tài khoản đã có hồ sơ bạn đọc", exception);
        }
        Map<String, Object> after = Map.of("userId", user.getId(), "status", patron.getStatus().name());
        auditService.record("LIBRARY_PATRON_ACTIVATED", ENTITY_TYPE, patron.getId(), null, after);
        return readModelService.patron(patron);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private LibraryPatronException error(HttpStatus status, String code, String message) {
        return new LibraryPatronException(status, code, message);
    }

    private LibraryPatronException error(HttpStatus status, String code, String message, Throwable cause) {
        return new LibraryPatronException(status, code, message, cause);
    }
}
