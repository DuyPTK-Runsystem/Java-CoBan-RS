package com.JavaTraining.BaiTap_RS.library.patron.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.requests.ReqActivateLibraryPatronDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.response.ResLibraryActivationCandidateDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.response.ResLibraryPatronDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.RoleCode;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryPatronServiceTest {

    private static final Long USER_ID = 15L;
    private static final Long PATRON_ID = 4L;

    @Mock
    private LibraryPatronRepository patronRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LibraryPatronReadModelService readModelService;

    @Mock
    private LibraryOperationAuditService auditService;

    @Mock
    private LibraryPatronPageableResolver pageableResolver;

    @InjectMocks
    private LibraryPatronActivationService service;

    @Test
    void activationCandidateServiceKeepsAdminAndLibrarianAccountsAvailableForPatronActivation() {
        User admin = account(21L, RoleCode.ADMIN);
        User librarian = account(22L, RoleCode.LIBRARIAN);
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id"));
        when(pageableResolver.candidates(0, 20)).thenReturn(pageable);
        when(userRepository.findActivationCandidates(null, pageable))
                .thenReturn(new PageImpl<>(List.of(admin, librarian), pageable, 2));
        when(readModelService.candidates(List.of(admin, librarian))).thenReturn(List.of(
                new ResLibraryActivationCandidateDTO(21L, "Admin", "user21", RoleCode.ADMIN.code()),
                new ResLibraryActivationCandidateDTO(22L, "Librarian", "user22", RoleCode.LIBRARIAN.code())));

        var page = service.activationCandidates(" ", 0, 20);

        assertEquals(List.of(RoleCode.ADMIN.code(), RoleCode.LIBRARIAN.code()), page.result().stream()
                .map(candidate -> candidate.roleCode()).toList());
        verify(userRepository).findActivationCandidates(null, pageable);
    }

    @Test
    void rejectsActivationWhenUserDoesNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        LibraryPatronException exception = assertThrows(LibraryPatronException.class,
                () -> service.activate(new ReqActivateLibraryPatronDTO(USER_ID)));

        assertEquals("PATRON_USER_NOT_FOUND", exception.getCode());
        verify(patronRepository, never()).saveAndFlush(any(LibraryPatron.class));
    }

    @Test
    void rejectsDuplicatePatronActivationWithoutSavingAnotherRecord() {
        User user = account(USER_ID, RoleCode.STUDENT);
        LibraryPatron existing = patron();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(patronRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));

        LibraryPatronException exception = assertThrows(LibraryPatronException.class,
                () -> service.activate(new ReqActivateLibraryPatronDTO(USER_ID)));

        assertEquals("PATRON_ALREADY_EXISTS", exception.getCode());
        verify(patronRepository, never()).saveAndFlush(any(LibraryPatron.class));
    }

    @Test
    void activatesUserAndRecordsAudit() {
        User user = account(USER_ID, RoleCode.STUDENT);
        LibraryPatron saved = patron();
        ResLibraryPatronDTO response = response(saved);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(patronRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(patronRepository.saveAndFlush(any(LibraryPatron.class))).thenReturn(saved);
        when(readModelService.patron(saved)).thenReturn(response);

        var result = service.activate(new ReqActivateLibraryPatronDTO(USER_ID));

        assertEquals(PATRON_ID, result.patronId());
        verify(auditService).record(eq("LIBRARY_PATRON_ACTIVATED"), eq("library_patron"), eq(PATRON_ID),
                org.mockito.ArgumentMatchers.isNull(), anyMap());
    }

    @Test
    void translatesConcurrentDuplicateActivationConstraintToConflict() {
        User user = account(USER_ID, RoleCode.STUDENT);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(patronRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(patronRepository.saveAndFlush(any(LibraryPatron.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate user_id"));

        LibraryPatronException exception = assertThrows(LibraryPatronException.class,
                () -> service.activate(new ReqActivateLibraryPatronDTO(USER_ID)));

        assertEquals("PATRON_ALREADY_EXISTS", exception.getCode());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    private User account(Long userId, RoleCode roleCode) {
        User user = new User("user" + userId, "not-a-real-password");
        ReflectionTestUtils.setField(user, "id", userId);
        user.addRole(new Role(roleCode.code(), roleCode.name(), null));
        return user;
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
