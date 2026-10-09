package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqReturnCopiesDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryReturnItemDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryReturnResultDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryLoanReturnService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final LibraryPolicyService policyService;
    private final LibraryLoanReturnPreparationService preparationService;
    private final LibraryLoanReturnItemService itemService;

    @Transactional
    public LibraryReturnResultDTO returnCopies(ReqReturnCopiesDTO request) {
        List<String> barcodes = normalize(request.copyBarcodes());
        List<BookCopy> copies = preparationService.lockCopiesForReturn(barcodes);
        LocalDateTime now = LocalDateTime.now(LIBRARY_ZONE);
        LocalDate today = now.toLocalDate();
        LibraryCirculationPolicy policy = policyService.resolveCurrent(now);
        List<LibraryReturnItemDTO> result = new ArrayList<>();
        for (BookCopy copy : copies) {
            result.add(itemService.returnCopy(copy, today, now, policy));
        }
        return new LibraryReturnResultDTO(result);
    }



    private List<String> normalize(List<String> values) {
        List<String> barcodes = values.stream().map(String::trim).toList();
        Set<String> unique = new HashSet<>(barcodes);
        if (unique.size() != barcodes.size() || unique.contains("")) {
            throw error(HttpStatus.BAD_REQUEST, "DUPLICATE_OR_EMPTY_BARCODE", "Barcode trùng hoặc để trống");
        }
        return barcodes;
    }

    private LibraryCirculationException error(HttpStatus status, String code, String message) {
        return new LibraryCirculationException(status, code, message);
    }
}
