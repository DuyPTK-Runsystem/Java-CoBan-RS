package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqReturnCopiesDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryReturnItemDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicySnapshot;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicyTerms;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryLoanReturnServiceTest {

    @Mock private LibraryPolicyService policyService;
    @Mock private LibraryLoanReturnPreparationService preparationService;
    @Mock private LibraryLoanReturnItemService itemService;

    @InjectMocks
    private LibraryLoanReturnService service;

    @Test
    void trimsBarcodeThenReturnsPreparedCopiesWithTheResolvedPolicy() {
        Book book = new Book("9780000000001", "Title", "Author", "Publisher", 2020, "Fiction",
                new BigDecimal("100000.00"), null);
        BookCopy copy = new BookCopy(book, "COPY-31", "Shelf", false);
        ReflectionTestUtils.setField(copy, "id", 31L);
        LibraryCirculationPolicy policy = policy();
        LibraryReturnItemDTO returnedItem = new LibraryReturnItemDTO(null, null);
        when(preparationService.lockCopiesForReturn(List.of("COPY-31"))).thenReturn(List.of(copy));
        when(policyService.resolveCurrent(any())).thenReturn(policy);
        when(itemService.returnCopy(eq(copy), any(), any(), eq(policy))).thenReturn(returnedItem);

        var result = service.returnCopies(new ReqReturnCopiesDTO(List.of(" COPY-31 ")));

        assertEquals(List.of(returnedItem), result.items());
        verify(preparationService).lockCopiesForReturn(List.of("COPY-31"));
        verify(itemService).returnCopy(eq(copy), any(), any(), eq(policy));
    }

    private LibraryCirculationPolicy policy() {
        LocalDateTime now = LocalDateTime.now();
        LibraryCirculationPolicyTerms terms = new LibraryCirculationPolicyTerms(5, 14, 2, 7, 3,
                new BigDecimal("500000.00"), new BigDecimal("500000.00"));
        return new LibraryCirculationPolicy(new LibraryCirculationPolicySnapshot(
                "LIB-POL-1", now, terms, 1L, now));
    }
}
