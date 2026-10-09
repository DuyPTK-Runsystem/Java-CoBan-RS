package com.JavaTraining.BaiTap_RS.library.patron.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryPatronFacadeServiceTest {

    private static final Long USER_ID = 15L;
    private static final Long PATRON_ID = 4L;

    @Mock
    private LibraryPatronRepository patronRepository;

    @Mock
    private LibraryPatronReadModelService readModelService;

    @Mock
    private LibraryPatronActivationService activationService;

    @Mock
    private LibraryPatronStatusService statusService;

    @Mock
    private LibraryAccessPolicy accessPolicy;

    @Mock
    private LibraryPatronPageableResolver pageableResolver;

    @InjectMocks
    private LibraryPatronService service;

    @Test
    void getsPatronByIdAndUsesReadModel() {
        LibraryPatron patron = patron();
        ResLibraryPatronDTO expected = response(patron);
        when(patronRepository.findById(PATRON_ID)).thenReturn(Optional.of(patron));
        when(readModelService.patron(patron)).thenReturn(expected);

        assertEquals(expected, service.get(PATRON_ID));
    }

    @Test
    void returnsNotFoundForMissingPatronId() {
        when(patronRepository.findById(PATRON_ID)).thenReturn(Optional.empty());

        LibraryPatronException exception = assertThrows(LibraryPatronException.class, () -> service.get(PATRON_ID));

        assertEquals("PATRON_NOT_FOUND", exception.getCode());
    }

    @Test
    void rejectsGetMeWhenThereIsNoAuthenticatedUser() {
        when(accessPolicy.currentUserId()).thenReturn(null);

        LibraryPatronException exception = assertThrows(LibraryPatronException.class, service::getMe);

        assertEquals("LIBRARY_RESOURCE_FORBIDDEN", exception.getCode());
    }

    @Test
    void getsCurrentPatronUsingAuthenticatedUserId() {
        LibraryPatron patron = patron();
        ResLibraryPatronDTO expected = response(patron);
        when(accessPolicy.currentUserId()).thenReturn(USER_ID);
        when(patronRepository.findByUserId(USER_ID)).thenReturn(Optional.of(patron));
        when(readModelService.patron(patron)).thenReturn(expected);

        assertEquals(expected, service.getMe());
    }

    @Test
    void searchesPatronsWithNormalizedKeywordAndResolvedPagination() {
        LibraryPatron patron = patron();
        ResLibraryPatronDTO expected = response(patron);
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "joinedAt"));
        when(pageableResolver.resolve(0, 20, null, null)).thenReturn(pageable);
        when(patronRepository.search("alice", LibraryPatronStatus.ACTIVE, pageable))
                .thenReturn(new PageImpl<>(List.of(patron), pageable, 1));
        when(readModelService.patrons(List.of(patron))).thenReturn(List.of(expected));

        var page = service.search(" alice ", LibraryPatronStatus.ACTIVE, 0, 20, null, null);

        assertEquals(List.of(expected), page.result());
        assertEquals(1, page.meta().totalItems());
    }

    @Test
    void delegatesActivationCandidateSearch() {
        ResLibraryActivationCandidateDTO candidate =
                new ResLibraryActivationCandidateDTO(USER_ID, "User", "user15", "STUDENT");
        var expected = new ResultPaginationDTO<>(
                new ResultPaginationDTO.Meta(0, 20, 1, 1),
                List.of(candidate));
        when(activationService.activationCandidates(" alice ", 0, 20)).thenReturn(expected);

        assertEquals(expected, service.activationCandidates(" alice ", 0, 20));
        verify(activationService).activationCandidates(" alice ", 0, 20);
    }

    @Test
    void delegatesPatronActivationAndStatusUpdates() {
        ReqActivateLibraryPatronDTO activation = new ReqActivateLibraryPatronDTO(USER_ID);
        ReqUpdateLibraryPatronStatusDTO status =
                new ReqUpdateLibraryPatronStatusDTO(LibraryPatronStatus.BORROWING_SUSPENDED, "Manual review");
        ResLibraryPatronDTO expected = response(patron());
        when(activationService.activate(activation)).thenReturn(expected);
        when(statusService.updateStatus(PATRON_ID, status)).thenReturn(expected);

        assertEquals(expected, service.activate(activation));
        assertEquals(expected, service.updateStatus(PATRON_ID, status));
        verify(activationService).activate(activation);
        verify(statusService).updateStatus(PATRON_ID, status);
    }

    private LibraryPatron patron() {
        LibraryPatron patron = new LibraryPatron(USER_ID, LocalDateTime.of(2026, 10, 9, 10, 0));
        ReflectionTestUtils.setField(patron, "id", PATRON_ID);
        return patron;
    }

    private ResLibraryPatronDTO response(LibraryPatron patron) {
        return new ResLibraryPatronDTO(patron.getId(), patron.getUserId(), "User", patron.getStatus(),
                patron.getJoinedAt(), List.of(), null);
    }
}
